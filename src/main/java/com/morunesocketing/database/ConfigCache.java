package com.morunesocketing.database;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 配置缓存
 * 减少数据库查询次数，提高性能
 */
public class ConfigCache {
    
    private final ConcurrentHashMap<String, CacheEntry> cache;
    private static final long DEFAULT_EXPIRE_TIME = TimeUnit.MINUTES.toMillis(5); // 默认5分钟过期
    
    public ConfigCache() {
        this.cache = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取缓存值
     */
    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        // 检查是否过期
        if (System.currentTimeMillis() > entry.expireTime) {
            cache.remove(key);
            return null;
        }
        
        return entry.value;
    }
    
    /**
     * 放入缓存
     */
    public void put(String key, String value) {
        put(key, value, DEFAULT_EXPIRE_TIME);
    }
    
    /**
     * 放入缓存，指定过期时间
     */
    public void put(String key, String value, long expireMillis) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis() + expireMillis));
    }
    
    /**
     * 移除缓存
     */
    public void remove(String key) {
        cache.remove(key);
    }
    
    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> now > entry.getValue().expireTime);
    }
    
    /**
     * 缓存条目
     */
    private static class CacheEntry {
        final String value;
        final long expireTime;
        
        CacheEntry(String value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
    }
}
