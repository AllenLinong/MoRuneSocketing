package com.morunesocketing;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时数据管理器
 * 纯内存存储，不创建任何配置文件
 * 支持数据库同步（如果启用）
 */
public class DataManager {
    
    private static DataManager instance;
    private final MoRuneSocketing plugin;
    
    // 随机成功率（内存存储）
    private volatile int currentSuccessRate = 100;
    private volatile long lastRefreshTime = 0;
    
    // 玩家打开菜单时的成功率缓存（解决菜单显示不同步问题）
    private final Map<UUID, Integer> playerMenuSuccessRate = new ConcurrentHashMap<>();
    
    // 跟踪哪些玩家打开了菜单（用于实时刷新）
    private final Map<UUID, Player> openMenuPlayers = new ConcurrentHashMap<>();
    
    public DataManager(MoRuneSocketing plugin) {
        this.plugin = plugin;
        instance = this;
    }
    
    public static DataManager getInstance() {
        return instance;
    }
    
    // ==================== 随机成功率相关 ====================
    
    /**
     * 获取当前随机成功率
     */
    public int getCurrentSuccessRate() {
        return currentSuccessRate;
    }
    
    /**
     * 设置当前随机成功率（手动设置，不更新刷新时间）
     */
    public void setCurrentSuccessRate(int rate) {
        int oldRate = this.currentSuccessRate;
        this.currentSuccessRate = rate;
        
        // 同步到数据库（如果启用）
        syncToDatabase();
        
        // 如果成功率变化了，刷新所有打开菜单的玩家
        if (oldRate != rate) {
            refreshAllOpenMenus();
        }
    }
    
    /**
     * 设置当前随机成功率（系统刷新，更新刷新时间）
     */
    public void setCurrentSuccessRateWithRefresh(int rate, long refreshTime) {
        int oldRate = this.currentSuccessRate;
        this.currentSuccessRate = rate;
        this.lastRefreshTime = refreshTime;
        
        // 同步到数据库（如果启用）
        syncToDatabase();
        
        // 如果成功率变化了，刷新所有打开菜单的玩家
        if (oldRate != rate) {
            refreshAllOpenMenus();
        }
    }
    
    /**
     * 获取上次刷新时间
     */
    public long getLastRefreshTime() {
        return lastRefreshTime;
    }
    
    /**
     * 设置上次刷新时间
     */
    public void setLastRefreshTime(long time) {
        this.lastRefreshTime = time;
    }
    
    /**
     * 从数据库加载数据到内存
     */
    public void loadFromDatabase() {
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isEnabled()) {
            // 数据库管理器会自动同步到内存
            plugin.getDatabaseManager().syncToMemory();
        }
    }
    
    /**
     * 同步到数据库
     */
    private void syncToDatabase() {
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isEnabled()) {
            int minRate = plugin.getConfig().getInt("features.random-success-rate.min-rate", 50);
            int maxRate = plugin.getConfig().getInt("features.random-success-rate.max-rate", 100);
            plugin.getDatabaseManager().saveSuccessRate(currentSuccessRate, minRate, maxRate, lastRefreshTime);
        }
    }
    
    /**
     * 刷新所有打开菜单的玩家
     */
    private void refreshAllOpenMenus() {
        for (Player player : openMenuPlayers.values()) {
            if (player != null && player.isOnline()) {
                RuneManager.refreshMenu(player);
            }
        }
    }
    
    // ==================== 玩家菜单跟踪 ====================
    
    /**
     * 记录玩家打开菜单
     */
    public void addOpenMenuPlayer(Player player) {
        openMenuPlayers.put(player.getUniqueId(), player);
    }
    
    /**
     * 移除玩家打开菜单记录
     */
    public void removeOpenMenuPlayer(UUID playerId) {
        openMenuPlayers.remove(playerId);
    }
    
    // ==================== 玩家菜单成功率缓存 ====================
    
    /**
     * 缓存玩家打开菜单时的成功率
     */
    public void cachePlayerMenuRate(UUID playerId, int rate) {
        playerMenuSuccessRate.put(playerId, rate);
    }
    
    /**
     * 获取玩家缓存的菜单成功率
     */
    public int getPlayerMenuRate(UUID playerId) {
        return playerMenuSuccessRate.getOrDefault(playerId, currentSuccessRate);
    }
    
    /**
     * 清除玩家的菜单成功率缓存
     */
    public void clearPlayerMenuRate(UUID playerId) {
        playerMenuSuccessRate.remove(playerId);
    }
    
    /**
     * 清除所有玩家的菜单成功率缓存
     */
    public void clearAllPlayerMenuRates() {
        playerMenuSuccessRate.clear();
    }
}
