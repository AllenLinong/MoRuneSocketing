package com.morunesocketing.economy;

import com.morunesocketing.MoRuneSocketing;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * RedisEconomy 支持
 * 使用反射避免硬依赖
 */
public class RedisEconomyProvider implements EconomyProvider {

    private MoRuneSocketing plugin;
    private Object redisEconomyAPI;
    private boolean enabled = false;

    public RedisEconomyProvider(MoRuneSocketing plugin) {
        this.plugin = plugin;

        if (Bukkit.getPluginManager().getPlugin("RedisEconomy") == null) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName("com.ivan1pl.rediseconomy.api.RedisEconomyAPI");
            redisEconomyAPI = apiClass.getMethod("getAPI").invoke(null);
            enabled = true;
        } catch (Exception e) {
            plugin.getLogger().warning("RedisEconomy 加载失败: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "RedisEconomy";
    }

    @Override
    public boolean isEnabled() {
        return enabled && redisEconomyAPI != null;
    }

    @Override
    public boolean has(Player player, double amount) {
        try {
            double balance = getBalance(player);
            return balance >= amount;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        try {
            Method method = redisEconomyAPI.getClass().getMethod("withdraw", OfflinePlayer.class, double.class);
            method.invoke(redisEconomyAPI, (OfflinePlayer) player, amount);
            return true;
        } catch (Exception e) {
            try {
                Method method = redisEconomyAPI.getClass().getMethod("withdraw", Player.class, double.class);
                method.invoke(redisEconomyAPI, player, amount);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    @Override
    public boolean deposit(Player player, double amount) {
        try {
            Method method = redisEconomyAPI.getClass().getMethod("deposit", OfflinePlayer.class, double.class);
            method.invoke(redisEconomyAPI, (OfflinePlayer) player, amount);
            return true;
        } catch (Exception e) {
            try {
                Method method = redisEconomyAPI.getClass().getMethod("deposit", Player.class, double.class);
                method.invoke(redisEconomyAPI, player, amount);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    @Override
    public double getBalance(Player player) {
        try {
            Method method = redisEconomyAPI.getClass().getMethod("getBalance", OfflinePlayer.class);
            return (double) method.invoke(redisEconomyAPI, (OfflinePlayer) player);
        } catch (Exception e) {
            try {
                Method method = redisEconomyAPI.getClass().getMethod("getBalance", Player.class);
                return (double) method.invoke(redisEconomyAPI, player);
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    @Override
    public String format(double amount) {
        return String.format("%.2f", amount);
    }

    @Override
    public String getCurrencyName() {
        return "金币";
    }
}
