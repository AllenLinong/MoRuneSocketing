package com.morunesocketing;

import com.morunesocketing.database.DatabaseManager;
import com.morunesocketing.economy.EconomyManager;
import com.morunesocketing.util.SchedulerUtils;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MoRuneSocketing extends JavaPlugin {
    
    private static MoRuneSocketing instance;
    private static final String PLUGIN_VERSION = "1.0.8-Release";
    private static final String AUTHOR = "Allen_Linong";
    private static final String QQ = "1422163791";
    private RuneListener runeListener;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private DataManager dataManager;
    private LanguageManager languageManager;

    private WrappedTask randomSuccessRateTask;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 保存默认配置文件
        saveDefaultConfig();
        
        // 保存其他分离的配置文件
        saveSeparatedConfigs();
        
        // 初始化语言管理器
        languageManager = new LanguageManager(this);
        languageManager.initialize();
        
        // 加载Buff检查配置
        RuneListener.loadConfig();
        
        // 初始化数据管理器
        dataManager = new DataManager(this);
        
        // 检测配置文件加载情况
        checkConfigurationStatus();
        
        // 初始化数据库
        initializeDatabase();
        
        // 初始化经济系统
        initializeEconomy();
        
        // 注册命令
        getCommand("mrs").setExecutor(new RuneCommand());
        getCommand("mrs").setTabCompleter(new RuneCommand());
        
        // 注册监听器
        runeListener = new RuneListener();
        getServer().getPluginManager().registerEvents(runeListener, this);
        
        // 启动buff检查任务
        runeListener.startBuffCheckTask();
        
        // 启动随机成功率刷新任务
        startRandomSuccessRateTask();
        
        // 显示启动完成信息
        printEnableMessage();
    }
    
    /**
     * 显示启动完成信息
     */
    private void printEnableMessage() {
        getLogger().info("▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃");
        getLogger().info("");
        getLogger().info("     [MoRuneSocketing] 插件已启用");
        getLogger().info("     版本: " + PLUGIN_VERSION);
        getLogger().info("");
        getLogger().info("     作者: " + AUTHOR);
        getLogger().info("     QQ: " + QQ);
        getLogger().info("");
        getLogger().info("▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃");
    }
    
    @Override
    public void onDisable() {
        // 停止随机成功率刷新任务
        stopRandomSuccessRateTask();
        
        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        // 关闭经济系统
        if (economyManager != null) {
            economyManager.shutdown();
        }
        
        // 显示关闭信息
        printDisableMessage();
    }
    
    /**
     * 显示关闭信息
     */
    private void printDisableMessage() {
        getLogger().info("▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃");
        getLogger().info("");
        getLogger().info("     [MoRuneSocketing] 插件已禁用");
        getLogger().info("     版本: " + PLUGIN_VERSION);
        getLogger().info("");
        getLogger().info("▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃");
    }
    
    /**
     * 保存分离的配置文件
     */
    private void saveSeparatedConfigs() {
        // 保存符文配置
        File runesFile = new File(getDataFolder(), "runes.yml");
        if (!runesFile.exists()) {
            saveResource("runes.yml", false);
        }
        
        // 保存菜单配置
        File menuConfigFile = new File(getDataFolder(), "menu-config.yml");
        if (!menuConfigFile.exists()) {
            saveResource("menu-config.yml", false);
        }
    }
    
    /**
     * 初始化数据库
     */
    private void initializeDatabase() {
        databaseManager = new DatabaseManager(this);
        if (databaseManager.initialize()) {
            getLogger().info("     ✔ 数据库系统初始化成功");
            String dbType = getConfig().getString("database.type", "sqlite");
            getLogger().info("       数据库类型: " + dbType.toUpperCase());
        } else {
            getLogger().info("     ✔ 使用本地数据存储模式");
        }
    }
    
    /**
     * 初始化经济系统
     */
    private void initializeEconomy() {
        economyManager = new EconomyManager(this);
        if (economyManager.initialize()) {
            getLogger().info("     ✔ 经济系统初始化成功");
        } else {
            getLogger().info("     ✔ 经济系统已禁用");
        }
    }
    
    /**
     * 启动随机成功率刷新任务
     */
    private void startRandomSuccessRateTask() {
        if (!getConfig().getBoolean("features.random-success-rate.enabled", false)) {
            return;
        }

        checkAndRefreshSuccessRate();

        long intervalTicks = parseIntervalToTicks(
            getConfig().getString("features.random-success-rate.refresh-interval", "1h")
        );

        this.randomSuccessRateTask = SchedulerUtils.runTaskTimer(this::refreshRandomSuccessRate, intervalTicks, intervalTicks);

        getLogger().info("随机成功率刷新任务已启动，刷新间隔: " + 
            getConfig().getString("features.random-success-rate.refresh-interval", "1h") +
            " (服务端: " + (SchedulerUtils.isFolia() ? "Folia" : "Spigot/Paper") + ")");
    }
    
    /**
     * 重启随机成功率刷新任务（公共方法，供命令调用）
     */
    public void restartRandomSuccessRateTask() {
        startRandomSuccessRateTask();
    }
    
    /**
     * 检查是否需要刷新成功率（仅在启动时调用）
     */
    private void checkAndRefreshSuccessRate() {
        // 如果启用了数据库，先从数据库同步
        if (databaseManager != null && databaseManager.isEnabled()) {
            dataManager.loadFromDatabase();
        }
        
        long lastRefresh = dataManager.getLastRefreshTime();
        long intervalMs = parseIntervalToMillis(
            getConfig().getString("features.random-success-rate.refresh-interval", "1h")
        );
        long currentTime = System.currentTimeMillis();
        
        // 如果从未刷新过，或者已经超过刷新间隔，则刷新
        if (lastRefresh == 0 || (currentTime - lastRefresh) >= intervalMs) {
            refreshRandomSuccessRate();
        } else {
            // 否则使用内存中的当前值
            int currentRate = dataManager.getCurrentSuccessRate();
            getLogger().info("使用当前成功率: " + currentRate + "%");
        }
    }
    
    /**
     * 解析时间间隔字符串为毫秒
     */
    private long parseIntervalToMillis(String interval) {
        if (interval == null || interval.isEmpty()) {
            return 3600000L; // 默认1小时
        }
        
        interval = interval.trim().toLowerCase();
        
        try {
            long result;
            if (interval.endsWith("d")) {
                int days = Integer.parseInt(interval.substring(0, interval.length() - 1));
                result = days * 24L * 60L * 60L * 1000L;
            } else if (interval.endsWith("h")) {
                int hours = Integer.parseInt(interval.substring(0, interval.length() - 1));
                result = hours * 60L * 60L * 1000L;
            } else if (interval.endsWith("m")) {
                int minutes = Integer.parseInt(interval.substring(0, interval.length() - 1));
                result = minutes * 60L * 1000L;
            } else if (interval.endsWith("s")) {
                int seconds = Integer.parseInt(interval.substring(0, interval.length() - 1));
                result = seconds * 1000L;
            } else {
                // 纯数字，假设为秒
                result = Long.parseLong(interval) * 1000L;
            }
            
            // 限制最低60秒
            return Math.max(result, 60000L);
            
        } catch (NumberFormatException e) {
            return 3600000L;
        }
    }
    
    /**
     * 停止随机成功率刷新任务
     */
    private void stopRandomSuccessRateTask() {
        if (randomSuccessRateTask != null) {
            randomSuccessRateTask.cancel();
            randomSuccessRateTask = null;
        }
    }
    
    /**
     * 刷新随机成功率
     */
    private void refreshRandomSuccessRate() {
        int minRate = getConfig().getInt("features.random-success-rate.min-rate", 50);
        int maxRate = getConfig().getInt("features.random-success-rate.max-rate", 100);
        
        // 确保范围有效
        if (maxRate < minRate) {
            maxRate = minRate;
        }
        
        // 生成随机成功率
        int randomRate = minRate + (int)(Math.random() * (maxRate - minRate + 1));
        long currentTime = System.currentTimeMillis();
        
        // 更新数据管理器中的值（内存存储，自动同步到数据库）
        dataManager.setCurrentSuccessRateWithRefresh(randomRate, currentTime);
        
        getLogger().info("随机成功率已刷新: " + randomRate + "%");
    }
    
    /**
     * 解析时间间隔字符串为tick数
     * 支持格式: 1d=1天, 1h=1小时, 30m=30分钟, 60s=60秒
     */
    public long parseIntervalToTicks(String interval) {
        if (interval == null || interval.isEmpty()) {
            return 72000L; // 默认1小时
        }
        
        interval = interval.trim().toLowerCase();
        
        try {
            long result;
            if (interval.endsWith("d")) {
                int days = Integer.parseInt(interval.substring(0, interval.length() - 1));
                result = days * 24L * 60L * 60L * 20L; // 天数转tick
            } else if (interval.endsWith("h")) {
                int hours = Integer.parseInt(interval.substring(0, interval.length() - 1));
                result = hours * 60L * 60L * 20L; // 小时转tick
            } else if (interval.endsWith("m")) {
                int minutes = Integer.parseInt(interval.substring(0, interval.length() - 1));
                result = minutes * 60L * 20L; // 分钟转tick
            } else if (interval.endsWith("s")) {
                int seconds = Integer.parseInt(interval.substring(0, interval.length() - 1));
                result = seconds * 20L; // 秒转tick
            } else {
                // 纯数字，假设为秒
                result = Long.parseLong(interval) * 20L;
            }
            
            // 限制最低60秒 (60 * 20 = 1200 ticks)
            return Math.max(result, 1200L);
            
        } catch (NumberFormatException e) {
            getLogger().warning("无法解析刷新间隔: " + interval + "，使用默认值1小时");
            return 72000L; // 默认1小时
        }
    }
    
    /**
     * 获取当前成功率
     */
    public int getCurrentSuccessRate() {
        // 检查是否启用随机成功率
        if (getConfig().getBoolean("features.random-success-rate.enabled", false)) {
            // 优先从数据库获取（如果启用）
            if (databaseManager != null && databaseManager.isEnabled()) {
                return databaseManager.getCurrentSuccessRate();
            }
            return dataManager.getCurrentSuccessRate();
        }
        // 返回固定成功率
        return getConfig().getInt("features.success-rate", 100);
    }
    
    /**
     * 获取数据管理器
     */
    public DataManager getDataManager() {
        return dataManager;
    }
    
    /**
     * 获取数据库管理器
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * 获取经济管理器
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    /**
     * 获取语言管理器
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    /**
     * 重载插件配置
     */
    public void reloadPlugin() {
        // 重新加载配置文件
        reloadConfig();
        
        // 重新加载语言文件
        if (languageManager != null) {
            languageManager.reload();
        }
        
        // 重新加载符文配置
        RuneManager.reloadRuneConfig();
        
        // 重新加载菜单配置
        RuneManager.reloadMenuConfig();
        
        // 停止现有任务
        stopRandomSuccessRateTask();
        
        // 关闭经济系统
        if (economyManager != null) {
            economyManager.shutdown();
        }
        
        // 从数据库重新加载数据到内存
        if (dataManager != null && databaseManager != null && databaseManager.isEnabled()) {
            dataManager.loadFromDatabase();
        }
        
        // 重新初始化数据库
        initializeDatabase();
        
        // 重新初始化经济系统
        initializeEconomy();
        
        // 重新启动随机成功率任务
        startRandomSuccessRateTask();
        
        // 清除所有配置缓存
        RuneManager.clearAllCaches();
        
        // 清除所有玩家的菜单成功率缓存
        if (dataManager != null) {
            dataManager.clearAllPlayerMenuRates();
        }
        
        getLogger().info("插件配置已重载!");
    }
    
    /**
     * 检测配置文件加载情况
     */
    private void checkConfigurationStatus() {
        getLogger().info("");
        getLogger().info("     配置文件检测:");
        getLogger().info("");
        
        try {
            checkMainConfig();
        } catch (Exception e) {
            getLogger().warning("     ✗ config.yml - 检测失败: " + e.getMessage());
        }
        
        try {
            checkRunesConfig();
        } catch (Exception e) {
            getLogger().warning("     ✗ runes.yml - 检测失败: " + e.getMessage());
        }
        
        try {
            checkMenuConfig();
        } catch (Exception e) {
            getLogger().warning("     ✗ menu-config.yml - 检测失败: " + e.getMessage());
        }
        
        try {
            checkLanguageConfig();
        } catch (Exception e) {
            getLogger().warning("     ✗ 语言文件 - 检测失败: " + e.getMessage());
        }
        
        getLogger().info("");
    }
    
    /**
     * 检测主配置文件
     */
    private void checkMainConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (configFile.exists()) {
            long fileSize = configFile.length();
            int keys = getConfig().getKeys(true).size();
            getLogger().info("     ✔ config.yml - 主配置文件");
            getLogger().info("       文件大小: " + formatFileSize(fileSize) + " | 配置项数量: " + keys);
            
            boolean dbEnabled = getConfig().getBoolean("database.enabled", false);
            boolean economyEnabled = getConfig().getBoolean("socketing.economy.enabled", false);
            boolean randomRateEnabled = getConfig().getBoolean("features.random-success-rate.enabled", false);
            
            getLogger().info("       数据库同步: " + (dbEnabled ? "启用" : "禁用"));
            getLogger().info("       经济系统: " + (economyEnabled ? "启用" : "禁用"));
            getLogger().info("       随机成功率: " + (randomRateEnabled ? "启用" : "禁用"));
        } else {
            getLogger().warning("     ✗ config.yml - 主配置文件不存在");
        }
    }
    
    /**
     * 检测符文配置文件
     */
    private void checkRunesConfig() {
        File runesFile = new File(getDataFolder(), "runes.yml");
        if (runesFile.exists()) {
            long fileSize = runesFile.length();
            org.bukkit.configuration.file.YamlConfiguration runesConfig = 
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(runesFile);
            
            int runeCount = runesConfig.getKeys(false).size();
            getLogger().info("     ✔ runes.yml - 符文配置文件");
            getLogger().info("       文件大小: " + formatFileSize(fileSize) + " | 符文总数: " + runeCount);
            
            if (runeCount > 0) {
                int enchantRunes = 0;
                int buffRunes = 0;
                for (String runeId : runesConfig.getKeys(false)) {
                    try {
                        org.bukkit.configuration.ConfigurationSection runeSection = 
                            runesConfig.getConfigurationSection(runeId);
                        if (runeSection != null) {
                            if (runeSection.contains("enchantment")) {
                                enchantRunes++;
                            }
                            if (runeSection.contains("buff")) {
                                buffRunes++;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                getLogger().info("       符文类型: 附魔符文 " + enchantRunes + "个 | 属性符文 " + buffRunes + "个");
            }
        } else {
            getLogger().warning("     ✗ runes.yml - 符文配置文件不存在");
        }
    }
    
    /**
     * 检测菜单配置文件
     */
    private void checkMenuConfig() {
        File menuFile = new File(getDataFolder(), "menu-config.yml");
        if (menuFile.exists()) {
            getLogger().info("     ✔ menu-config.yml - 菜单配置文件");
        } else {
            getLogger().warning("     ✗ menu-config.yml - 菜单配置文件不存在");
        }
    }
    
    /**
     * 检测语言配置文件
     */
    private void checkLanguageConfig() {
        String lang = getConfig().getString("language", "zh_CN");
        File langFile = new File(getDataFolder(), "lang/" + lang + ".yml");
        if (langFile.exists()) {
            long fileSize = langFile.length();
            getLogger().info("     ✔ lang/" + lang + ".yml - 语言文件");
            getLogger().info("       文件大小: " + formatFileSize(fileSize));
        } else {
            getLogger().warning("     ✗ lang/" + lang + ".yml - 语言文件不存在");
        }
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    public static MoRuneSocketing getInstance() {
        return instance;
    }
    
    public static String getPluginVersion() {
        return PLUGIN_VERSION;
    }
}
