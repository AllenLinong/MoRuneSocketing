package com.morunesocketing.database;

import com.morunesocketing.MoRuneSocketing;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * 数据库管理器
 * 负责数据库连接、数据同步和缓存管理
 */
public class DatabaseManager {
    
    private static DatabaseManager instance;
    private MoRuneSocketing plugin;
    private ConnectionPool connectionPool;
    private ScheduledExecutorService syncScheduler;
    private ConfigCache configCache;
    
    // 缓存最近的数据库值，减少查询次数
    private volatile int cachedSuccessRate = -1;
    private volatile long lastSyncTime = 0;
    private final Object syncLock = new Object();
    
    public DatabaseManager(MoRuneSocketing plugin) {
        this.plugin = plugin;
        this.configCache = new ConfigCache();
    }
    
    public static DatabaseManager getInstance() {
        return instance;
    }
    
    /**
     * 初始化数据库
     */
    public boolean initialize() {
        if (!plugin.getConfig().getBoolean("database.enabled", false)) {
            return false;
        }
        
        try {
            // 加载外部驱动
            loadExternalDrivers();
            
            String dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
            
            if (dbType.equals("mysql")) {
                connectionPool = new MySQLConnectionPool(plugin);
            } else {
                connectionPool = new SQLiteConnectionPool(plugin);
            }
            
            // 测试连接
            if (!connectionPool.testConnection()) {
                plugin.getLogger().severe("数据库连接失败，将使用本地配置文件");
                return false;
            }
            
            // 初始化表结构
            initializeTables();
            
            // 启动同步任务
            startSyncTask();
            
            instance = this;
            plugin.getLogger().info("数据库管理器初始化成功 (类型: " + dbType + ")");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "数据库初始化失败", e);
            return false;
        }
    }
    
    /**
     * 初始化数据库表
     */
    private void initializeTables() {
        String createSuccessRateTable = """
            CREATE TABLE IF NOT EXISTS morune_success_rate (
                id INT PRIMARY KEY DEFAULT 1,
                current_rate INT NOT NULL DEFAULT 100,
                min_rate INT NOT NULL DEFAULT 50,
                max_rate INT NOT NULL DEFAULT 100,
                last_refresh BIGINT NOT NULL DEFAULT 0,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                CHECK (id = 1)
            )
            """;
        
        String createConfigTable = """
            CREATE TABLE IF NOT EXISTS morune_config (
                config_key VARCHAR(64) PRIMARY KEY,
                config_value TEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;
        
        String insertDefaultSuccessRate = """
            INSERT INTO morune_success_rate (id, current_rate, min_rate, max_rate, last_refresh)
            VALUES (1, 100, 50, 100, 0)
            ON DUPLICATE KEY UPDATE id = id
            """;
        
        try (Connection conn = connectionPool.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createSuccessRateTable);
            stmt.execute(createConfigTable);
            stmt.execute(insertDefaultSuccessRate);
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "创建数据库表失败", e);
        }
    }
    
    /**
     * 启动同步任务
     */
    private void startSyncTask() {
        int syncInterval = plugin.getConfig().getInt("database.sync.interval", 60);
        
        syncScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MoRune-DatabaseSync");
            t.setDaemon(true);
            return t;
        });
        
        // 定期从数据库同步到本地
        syncScheduler.scheduleAtFixedRate(this::syncFromDatabase, 
            syncInterval, syncInterval, TimeUnit.SECONDS);
        
        // 立即同步一次
        syncFromDatabase();
        
        plugin.getLogger().info("数据库同步任务已启动 (间隔: " + syncInterval + "秒)");
    }
    
    /**
     * 从数据库同步数据到内存
     */
    private void syncFromDatabase() {
        if (!plugin.getConfig().getBoolean("database.sync.sync-success-rate", true)) {
            return;
        }
        
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT current_rate, last_refresh FROM morune_success_rate WHERE id = 1")) {
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int dbSuccessRate = rs.getInt("current_rate");
                long dbLastRefresh = rs.getLong("last_refresh");
                
                synchronized (syncLock) {
                    // 只有当数据库的值更新时，才更新本地缓存
                    if (dbLastRefresh > lastSyncTime) {
                        cachedSuccessRate = dbSuccessRate;
                        lastSyncTime = dbLastRefresh;
                        
                        // 更新数据管理器中的内存值
                        if (plugin.getDataManager() != null) {
                            plugin.getDataManager().setCurrentSuccessRateWithRefresh(dbSuccessRate, dbLastRefresh);
                        }
                        
                        plugin.getLogger().fine("从数据库同步成功率: " + dbSuccessRate + "%");
                    }
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "从数据库同步数据失败", e);
        }
    }
    
    /**
     * 从数据库同步数据到内存（启动时调用）
     */
    public void syncToMemory() {
        syncFromDatabase();
    }
    
    /**
     * 保存成功率到数据库
     */
    public void saveSuccessRate(int currentRate, int minRate, int maxRate, long lastRefresh) {
        if (!isEnabled()) return;
        
        // 异步保存，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO morune_success_rate (id, current_rate, min_rate, max_rate, last_refresh)
                     VALUES (1, ?, ?, ?, ?)
                     ON DUPLICATE KEY UPDATE
                     current_rate = VALUES(current_rate),
                     min_rate = VALUES(min_rate),
                     max_rate = VALUES(max_rate),
                     last_refresh = VALUES(last_refresh)
                     """)) {
                
                stmt.setInt(1, currentRate);
                stmt.setInt(2, minRate);
                stmt.setInt(3, maxRate);
                stmt.setLong(4, lastRefresh);
                stmt.executeUpdate();
                
                synchronized (syncLock) {
                    cachedSuccessRate = currentRate;
                    lastSyncTime = lastRefresh;
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "保存成功率到数据库失败", e);
            }
        });
    }
    
    /**
     * 获取当前成功率
     */
    public int getCurrentSuccessRate() {
        if (!isEnabled()) {
            // 从数据管理器获取
            if (plugin.getDataManager() != null) {
                return plugin.getDataManager().getCurrentSuccessRate();
            }
            return 100;
        }
        
        // 优先使用缓存
        if (cachedSuccessRate >= 0) {
            return cachedSuccessRate;
        }
        
        // 从数据库查询
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT current_rate FROM morune_success_rate WHERE id = 1")) {
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                cachedSuccessRate = rs.getInt("current_rate");
                return cachedSuccessRate;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "从数据库获取成功率失败", e);
        }
        
        // 回退到数据管理器
        if (plugin.getDataManager() != null) {
            return plugin.getDataManager().getCurrentSuccessRate();
        }
        return 100;
    }
    
    /**
     * 保存配置项到数据库
     */
    public void saveConfig(String key, String value) {
        if (!isEnabled() || !plugin.getConfig().getBoolean("database.sync.sync-config", true)) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO morune_config (config_key, config_value)
                     VALUES (?, ?)
                     ON DUPLICATE KEY UPDATE
                     config_value = VALUES(config_value)
                     """)) {
                
                stmt.setString(1, key);
                stmt.setString(2, value);
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "保存配置到数据库失败: " + key, e);
            }
        });
    }
    
    /**
     * 从数据库加载配置
     */
    public String loadConfig(String key, String defaultValue) {
        if (!isEnabled() || !plugin.getConfig().getBoolean("database.sync.sync-config", true)) {
            return defaultValue;
        }
        
        // 先检查缓存
        String cached = configCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT config_value FROM morune_config WHERE config_key = ?")) {
            
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String value = rs.getString("config_value");
                configCache.put(key, value);
                return value;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "从数据库加载配置失败: " + key, e);
        }
        
        return defaultValue;
    }
    
    /**
     * 检查数据库是否启用
     */
    public boolean isEnabled() {
        return connectionPool != null && plugin.getConfig().getBoolean("database.enabled", false);
    }
    
    /**
     * 关闭数据库连接
     */
    public void shutdown() {
        if (syncScheduler != null) {
            syncScheduler.shutdown();
            try {
                if (!syncScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    syncScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                syncScheduler.shutdownNow();
            }
            syncScheduler = null;
        }
        
        if (connectionPool != null) {
            connectionPool.close();
            connectionPool = null;
        }
        
        // 清除缓存
        cachedSuccessRate = -1;
        lastSyncTime = 0;
        
        // 清除静态实例
        instance = null;
        
        plugin.getLogger().info("数据库管理器已关闭");
    }
    
    /**
     * 获取连接池状态
     */
    public String getPoolStatus() {
        if (connectionPool == null) {
            return "未初始化";
        }
        return connectionPool.getStatus();
    }
    
    /**
     * 加载外部数据库驱动
     * 从 plugins/MoRuneSocketing/libs/ 目录加载
     */
    private void loadExternalDrivers() {
        File libsDir = new File(plugin.getDataFolder(), "libs");
        if (!libsDir.exists()) {
            libsDir.mkdirs();
            plugin.getLogger().info("创建外部驱动目录: " + libsDir.getAbsolutePath());
            plugin.getLogger().info("如需使用数据库功能，请将驱动文件放入此目录");
            return;
        }
        
        File[] jarFiles = libsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            plugin.getLogger().info("未找到外部驱动文件，将使用服务器内置驱动");
            return;
        }
        
        try {
            URL[] urls = new URL[jarFiles.length];
            for (int i = 0; i < jarFiles.length; i++) {
                urls[i] = jarFiles[i].toURI().toURL();
                plugin.getLogger().info("加载外部驱动: " + jarFiles[i].getName());
            }
            
            URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader());
            Thread.currentThread().setContextClassLoader(classLoader);
            
            plugin.getLogger().info("成功加载 " + jarFiles.length + " 个外部驱动");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "加载外部驱动失败，将使用服务器内置驱动", e);
        }
    }
}
