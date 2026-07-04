package com.morunesocketing.database;

import com.morunesocketing.MoRuneSocketing;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * SQLite连接池实现
 * 轻量级，适合单服务器使用
 */
public class SQLiteConnectionPool implements ConnectionPool {
    
    private final MoRuneSocketing plugin;
    private final String dbUrl;
    private final BlockingQueue<Connection> connectionPool;
    private final int poolSize = 3; // SQLite不需要太多连接
    private volatile boolean closed = false;
    
    public SQLiteConnectionPool(MoRuneSocketing plugin) {
        this.plugin = plugin;
        this.connectionPool = new ArrayBlockingQueue<>(poolSize);
        
        // 初始化数据库文件路径
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File dbFile = new File(dataFolder, "morune.db");
        this.dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        
        // 初始化连接池
        initializePool();
    }
    
    private void initializePool() {
        try {
            Class.forName("org.sqlite.JDBC");
            
            for (int i = 0; i < poolSize; i++) {
                Connection conn = createConnection();
                if (conn != null) {
                    connectionPool.offer(conn);
                }
            }
            
            plugin.getLogger().info("SQLite连接池已初始化 (大小: " + poolSize + ")");
            
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite驱动未找到", e);
        }
    }
    
    private Connection createConnection() {
        try {
            Connection conn = DriverManager.getConnection(dbUrl);
            // 优化SQLite性能
            conn.createStatement().execute("PRAGMA journal_mode = WAL");
            conn.createStatement().execute("PRAGMA synchronous = NORMAL");
            conn.createStatement().execute("PRAGMA cache_size = 10000");
            return conn;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "创建SQLite连接失败", e);
            return null;
        }
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("连接池已关闭");
        }
        
        try {
            Connection conn = connectionPool.poll(5, TimeUnit.SECONDS);
            if (conn == null || conn.isClosed()) {
                // 如果连接无效，创建新连接
                conn = createConnection();
            }
            
            final Connection finalConn = conn;
            return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close")) {
                        // 归还连接到池
                        if (!closed && finalConn != null && !finalConn.isClosed()) {
                            connectionPool.offer(finalConn);
                        }
                        return null;
                    }
                    return method.invoke(finalConn, args);
                }
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("获取连接超时", e);
        }
    }
    
    @Override
    public boolean testConnection() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "SQLite连接测试失败", e);
            return false;
        }
    }
    
    @Override
    public void close() {
        closed = true;
        
        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "关闭SQLite连接失败", e);
            }
        }
        
        plugin.getLogger().info("SQLite连接池已关闭");
    }
    
    @Override
    public String getStatus() {
        return "SQLite | 可用连接: " + connectionPool.size() + "/" + poolSize;
    }
}
