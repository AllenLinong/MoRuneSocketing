package com.morunesocketing;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 语言管理器
 * 负责加载和管理多语言消息
 */
public class LanguageManager {
    
    private final MoRuneSocketing plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String currentLanguage;
    private YamlConfiguration langConfig;
    
    public LanguageManager(MoRuneSocketing plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 初始化语言管理器
     */
    public void initialize() {
        // 获取配置的语言
        currentLanguage = plugin.getConfig().getString("language", "zh_CN");
        
        // 保存默认语言文件
        saveDefaultLanguageFiles();
        
        // 加载语言文件
        loadLanguage();
        
        plugin.getLogger().info("语言系统已加载: " + currentLanguage);
    }
    
    /**
     * 保存默认语言文件
     */
    private void saveDefaultLanguageFiles() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        
        // 保存中文语言文件
        File zhCNFile = new File(langFolder, "zh_CN.yml");
        if (!zhCNFile.exists()) {
            saveResource("lang/zh_CN.yml", zhCNFile);
        }
        
        // 保存英文语言文件
        File enUSFile = new File(langFolder, "en_US.yml");
        if (!enUSFile.exists()) {
            saveResource("lang/en_US.yml", enUSFile);
        }
    }
    
    /**
     * 保存资源文件
     */
    private void saveResource(String resourcePath, File targetFile) {
        try (InputStream is = plugin.getResource(resourcePath)) {
            if (is == null) {
                plugin.getLogger().warning("无法找到资源文件: " + resourcePath);
                return;
            }
            
            java.nio.file.Files.copy(is, targetFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().warning("保存资源文件失败: " + resourcePath + " - " + e.getMessage());
        }
    }
    
    /**
     * 加载语言文件
     */
    public void loadLanguage() {
        messages.clear();
        
        File langFile = new File(plugin.getDataFolder(), "lang/" + currentLanguage + ".yml");
        
        if (!langFile.exists()) {
            plugin.getLogger().warning("语言文件不存在: " + currentLanguage + "，使用默认中文");
            currentLanguage = "zh_CN";
            langFile = new File(plugin.getDataFolder(), "lang/zh_CN.yml");
            
            if (!langFile.exists()) {
                plugin.getLogger().warning("默认语言文件也不存在，使用内置默认值");
                loadDefaultMessages();
                return;
            }
        }
        
        try {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
            
            // 设置默认值（从jar包内的资源文件）
            InputStream defaultStream = plugin.getResource("lang/" + currentLanguage + ".yml");
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                langConfig.setDefaults(defaultConfig);
            }
            
            // 加载所有消息
            loadMessages("", langConfig);
            
        } catch (Exception e) {
            plugin.getLogger().warning("加载语言文件失败: " + e.getMessage());
            loadDefaultMessages();
        }
    }
    
    /**
     * 递归加载消息
     */
    private void loadMessages(String path, YamlConfiguration config) {
        for (String key : config.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            
            if (config.isConfigurationSection(key)) {
                loadMessages(fullPath, config.getConfigurationSection(key));
            } else {
                String value = config.getString(key);
                if (value != null) {
                    messages.put(fullPath, value);
                }
            }
        }
    }
    
    /**
     * 递归加载消息（ConfigurationSection版本）
     */
    private void loadMessages(String path, org.bukkit.configuration.ConfigurationSection section) {
        if (section == null) return;
        
        for (String key : section.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            
            if (section.isConfigurationSection(key)) {
                loadMessages(fullPath, section.getConfigurationSection(key));
            } else {
                String value = section.getString(key);
                if (value != null) {
                    messages.put(fullPath, value);
                }
            }
        }
    }
    
    /**
     * 加载默认消息
     */
    private void loadDefaultMessages() {
        messages.put("prefix", "§8[§6MoRune§8] §r");
        messages.put("no-permission", "§c你没有权限！");
        messages.put("player-only", "§c该命令只能由玩家执行！");
        messages.put("insert-success", "§a符文镶嵌成功！");
        messages.put("insert-failed", "§c符文镶嵌失败！");
        messages.put("max-buff-runes", "§c该装备已达到最大buff符文数量限制！");
        messages.put("missing-enchantment", "§c该装备需要先拥有相同附魔才能使用此符文！");
        messages.put("insufficient-funds", "§c你的金币不足！需要 §e%cost% §c金币");
        messages.put("invalid-rune-or-item", "§c符文或装备无效！");
        messages.put("invalid-rune-config", "§c符文配置无效，无法镶嵌！");
        messages.put("max-enchantment-level", "§c该装备的相同附魔已达到该符文的最大等级限制！");
        messages.put("invalid-enchantment-type", "§c无效的附魔类型：%enchantment%！");
        messages.put("buff-apply-unknown-error", "§c无法应用BUFF，未知原因！");
        messages.put("enchantment-apply-unknown-error", "§c无法应用附魔，未知原因！");
        messages.put("same-buff-level", "§c该装备已拥有相同等级的BUFF符文，无法镶嵌！");
        messages.put("lower-buff-level", "§c该装备已拥有更高等级的BUFF符文（当前：%current_level%级，符文：%rune_level%级），无法镶嵌！");
        messages.put("menu.disabled", "§c镶嵌功能已禁用！");
        messages.put("menu.config-error", "§c菜单配置文件不存在或加载失败！");
        messages.put("menu.invalid-item", "§c请放入有效的装备！");
        messages.put("menu.invalid-equipment", "§c该物品无法附魔！");
        messages.put("menu.invalid-rune", "§c请放入有效的符文！");
        messages.put("menu.wrong-rune", "§c请放入正确的符文！");
        messages.put("menu.cannot-socket-rune", "§c该物品无法镶嵌符文！");
        messages.put("command.unknown", "§c未知命令！输入 /mrs help 查看帮助。");
        messages.put("command.no-admin-permission", "§c你没有权限使用该命令！");
        messages.put("command.setrate.disabled", "§c随机成功率功能未启用！");
        messages.put("command.setrate.usage", "§c用法: /mrs setrate <成功率>");
        messages.put("command.setrate.current", "§e当前随机成功率: §a%rate%%");
        messages.put("command.setrate.invalid-number", "§c成功率必须是数字！");
        messages.put("command.setrate.out-of-range", "§c成功率必须在 %min%% 到 %max%% 之间！");
        messages.put("command.setrate.success", "§a成功率已设置为: §e%rate%%");
        messages.put("command.setrate.time-unchanged", "§7下次刷新时间不变，将继续按原定时间刷新。");
        messages.put("command.setinterval.disabled", "§c随机成功率功能未启用！");
        messages.put("command.setinterval.usage", "§c用法: /mrs setinterval <时间> [单位]");
        messages.put("command.setinterval.invalid-time", "§c时间必须是正整数！");
        messages.put("command.setinterval.invalid-unit", "§c无效的时间单位！支持: s(秒), m(分钟), h(小时), d(天)");
        messages.put("command.setinterval.too-short", "§c刷新间隔不能少于60秒！");
        messages.put("command.setinterval.success", "§a随机成功率刷新间隔已设置为: §e%time%%unit%");
        messages.put("command.give.usage", "§c用法: /mrs give <玩家> <符文ID> [数量]");
        messages.put("command.give.player-not-found", "§c找不到玩家: §e%player%");
        messages.put("command.give.rune-not-found", "§c找不到符文: §e%rune%");
        messages.put("command.give.invalid-amount", "§c数量必须是正整数！");
        messages.put("command.give.success", "§a成功给予 §e%player% §a%amount% 个 §e%rune%§a！");
        messages.put("command.reload.success", "§a配置文件已重载！");
    }
    
    /**
     * 获取消息
     * @param path 消息路径
     * @return 消息内容
     */
    public String getMessage(String path) {
        return messages.getOrDefault(path, "§c消息未找到: " + path);
    }
    
    /**
     * 获取消息并替换变量
     * @param path 消息路径
     * @param replacements 替换变量 (key1, value1, key2, value2, ...)
     * @return 替换后的消息
     */
    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        return message;
    }
    
    /**
     * 获取前缀
     * @return 消息前缀
     */
    public String getPrefix() {
        return getMessage("prefix");
    }
    
    /**
     * 发送带前缀的消息
     */
    public void sendMessage(org.bukkit.command.CommandSender sender, String path) {
        sender.sendMessage(getPrefix() + getMessage(path));
    }
    
    /**
     * 发送带前缀和变量的消息
     */
    public void sendMessage(org.bukkit.command.CommandSender sender, String path, String... replacements) {
        sender.sendMessage(getPrefix() + getMessage(path, replacements));
    }
    
    /**
     * 重载语言文件
     */
    public void reload() {
        currentLanguage = plugin.getConfig().getString("language", "zh_CN");
        loadLanguage();
        plugin.getLogger().info("语言文件已重载: " + currentLanguage);
    }
    
    /**
     * 获取当前语言
     * @return 当前语言代码
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
