package com.morunesocketing.economy;

import org.bukkit.entity.Player;

/**
 * 经济提供商接口
 */
public interface EconomyProvider {
    
    /**
     * 获取提供商名称
     */
    String getName();
    
    /**
     * 检查是否已启用
     */
    boolean isEnabled();
    
    /**
     * 检查玩家是否有足够的余额
     */
    boolean has(Player player, double amount);
    
    /**
     * 扣除玩家余额
     */
    boolean withdraw(Player player, double amount);
    
    /**
     * 给玩家增加余额
     */
    boolean deposit(Player player, double amount);
    
    /**
     * 获取玩家余额
     */
    double getBalance(Player player);
    
    /**
     * 格式化金额
     */
    String format(double amount);
    
    /**
     * 获取货币名称
     */
    String getCurrencyName();
}
