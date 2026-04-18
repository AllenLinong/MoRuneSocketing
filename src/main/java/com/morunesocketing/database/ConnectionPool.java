package com.morunesocketing.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接池接口
 */
public interface ConnectionPool {
    
    /**
     * 获取数据库连接
     */
    Connection getConnection() throws SQLException;
    
    /**
     * 测试数据库连接
     */
    boolean testConnection();
    
    /**
     * 关闭连接池
     */
    void close();
    
    /**
     * 获取连接池状态
     */
    String getStatus();
}
