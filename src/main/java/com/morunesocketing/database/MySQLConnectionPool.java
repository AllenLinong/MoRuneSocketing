package com.morunesocketing.database;

import com.morunesocketing.MoRuneSocketing;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * MySQL连接池实现
 * 使用HikariCP高性能连接池
 */
public class MySQLConnectionPool implements ConnectionPool {
    
    private final MoRuneSocketing plugin;
    private HikariDataSource dataSource;
    
    public MySQLConnectionPool(MoRuneSocketing plugin) {
        this.plugin = plugin;
        initializePool();
    }
    
    private void initializePool() {
        try {
            HikariConfig config = new HikariConfig();
            
            // 从配置读取连接信息
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "morune");
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "");
            boolean useSSL = plugin.getConfig().getBoolean("database.mysql.use-ssl", false);
            
            // 连接池配置
            int minConnections = plugin.getConfig().getInt("database.mysql.pool.min-connections", 2);
            int maxConnections = plugin.getConfig().getInt("database.mysql.pool.max-connections", 10);
            int connectionTimeout = plugin.getConfig().getInt("database.mysql.pool.connection-timeout", 30000);
            
            // 构建JDBC URL
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=%b&characterEncoding=UTF-8&autoReconnect=true",
                host, port, database, useSSL);
            
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            
            // 连接池优化配置
            config.setMinimumIdle(minConnections);
            config.setMaximumPoolSize(maxConnections);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(600000); // 10分钟
            config.setMaxLifetime(1800000); // 30分钟
            config.setLeakDetectionThreshold(60000); // 连接泄漏检测
            
            // 性能优化
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            dataSource = new HikariDataSource(config);
            
            plugin.getLogger().info("MySQL连接池已初始化 (" + host + ":" + port + "/" + database + ")");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "MySQL连接池初始化失败", e);
        }
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("连接池未初始化或已关闭");
        }
        return dataSource.getConnection();
    }
    
    @Override
    public boolean testConnection() {
        if (dataSource == null) {
            return false;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "MySQL连接测试失败", e);
            return false;
        }
    }
    
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("MySQL连接池已关闭");
        }
    }
    
    @Override
    public String getStatus() {
        if (dataSource == null) {
            return "MySQL | 未初始化";
        }
        return String.format("MySQL | 活跃连接: %d/%d | 空闲连接: %d | 等待线程: %d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getMaximumPoolSize(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
    }
}
