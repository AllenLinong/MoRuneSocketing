package com.morunesocketing.economy;

import com.morunesocketing.MoRuneSocketing;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * PEconomy 支持
 * 使用反射避免硬依赖
 */
public class PEconomyProvider implements EconomyProvider {

    private MoRuneSocketing plugin;
    private Object peconomyAPI;
    private boolean enabled = false;

    public PEconomyProvider(MoRuneSocketing plugin) {
        this.plugin = plugin;

        if (Bukkit.getPluginManager().getPlugin("PEconomy") == null) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName("cn.pigeonmc.pigeon.economy.api.PEconomyAPI");
            peconomyAPI = apiClass.getMethod("getInstance").invoke(null);
            enabled = true;
        } catch (Exception e) {
            plugin.getLogger().warning("PEconomy 加载失败: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "PEconomy";
    }

    @Override
    public boolean isEnabled() {
        return enabled && peconomyAPI != null;
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
            Method method = peconomyAPI.getClass().getMethod("withdraw", OfflinePlayer.class, double.class);
            method.invoke(peconomyAPI, (OfflinePlayer) player, amount);
            return true;
        } catch (Exception e) {
            try {
                Method method = peconomyAPI.getClass().getMethod("withdraw", Player.class, double.class);
                method.invoke(peconomyAPI, player, amount);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    @Override
    public boolean deposit(Player player, double amount) {
        try {
            Method method = peconomyAPI.getClass().getMethod("deposit", OfflinePlayer.class, double.class);
            method.invoke(peconomyAPI, (OfflinePlayer) player, amount);
            return true;
        } catch (Exception e) {
            try {
                Method method = peconomyAPI.getClass().getMethod("deposit", Player.class, double.class);
                method.invoke(peconomyAPI, player, amount);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    @Override
    public double getBalance(Player player) {
        try {
            Method method = peconomyAPI.getClass().getMethod("getBalance", OfflinePlayer.class);
            return (double) method.invoke(peconomyAPI, (OfflinePlayer) player);
        } catch (Exception e) {
            try {
                Method method = peconomyAPI.getClass().getMethod("getBalance", Player.class);
                return (double) method.invoke(peconomyAPI, player);
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
