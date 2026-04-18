package com.morunesocketing.economy;

import com.morunesocketing.MoRuneSocketing;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * PlayerPoints 点券支持
 * 使用反射避免硬依赖
 */
public class PlayerPointsProvider implements EconomyProvider {
    
    private MoRuneSocketing plugin;
    private Object playerPointsAPI;
    private boolean enabled = false;
    
    public PlayerPointsProvider(MoRuneSocketing plugin) {
        this.plugin = plugin;
        
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            return;
        }
        
        try {
            // 使用反射获取API
            Class<?> playerPointsClass = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
            Object playerPoints = Bukkit.getPluginManager().getPlugin("PlayerPoints");
            playerPointsAPI = playerPointsClass.getMethod("getAPI").invoke(playerPoints);
            enabled = true;
        } catch (Exception e) {
            plugin.getLogger().warning("PlayerPoints 加载失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getName() {
        return "PlayerPoints";
    }
    
    @Override
    public boolean isEnabled() {
        return enabled && playerPointsAPI != null;
    }
    
    @Override
    public boolean has(Player player, double amount) {
        try {
            int balance = (int) playerPointsAPI.getClass()
                    .getMethod("look", UUID.class)
                    .invoke(playerPointsAPI, player.getUniqueId());
            return balance >= (int) amount;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean withdraw(Player player, double amount) {
        try {
            return (boolean) playerPointsAPI.getClass()
                    .getMethod("take", UUID.class, int.class)
                    .invoke(playerPointsAPI, player.getUniqueId(), (int) amount);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean deposit(Player player, double amount) {
        try {
            return (boolean) playerPointsAPI.getClass()
                    .getMethod("give", UUID.class, int.class)
                    .invoke(playerPointsAPI, player.getUniqueId(), (int) amount);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public double getBalance(Player player) {
        try {
            return (int) playerPointsAPI.getClass()
                    .getMethod("look", UUID.class)
                    .invoke(playerPointsAPI, player.getUniqueId());
        } catch (Exception e) {
            return 0;
        }
    }
    
    @Override
    public String format(double amount) {
        return String.valueOf((int) amount);
    }
    
    @Override
    public String getCurrencyName() {
        return "点券";
    }
}
