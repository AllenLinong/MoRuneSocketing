package com.morunesocketing.economy;

import com.morunesocketing.MoRuneSocketing;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * GemsEconomy 支持
 * 使用反射避免硬依赖
 */
public class GemsEconomyProvider implements EconomyProvider {

    private MoRuneSocketing plugin;
    private Object gemsAPI;
    private Object defaultCurrency;
    private boolean enabled = false;

    public GemsEconomyProvider(MoRuneSocketing plugin) {
        this.plugin = plugin;

        if (Bukkit.getPluginManager().getPlugin("GemsEconomy") == null) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName("me.xanium.gemseconomy.api.GemsEconomyAPI");
            gemsAPI = apiClass.getConstructor().newInstance();
            
            Method getDefaultCurrency = apiClass.getMethod("getDefaultCurrency");
            defaultCurrency = getDefaultCurrency.invoke(gemsAPI);
            enabled = true;
        } catch (Exception e) {
            plugin.getLogger().warning("GemsEconomy 加载失败: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "GemsEconomy";
    }

    @Override
    public boolean isEnabled() {
        return enabled && gemsAPI != null && defaultCurrency != null;
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
            Method method = gemsAPI.getClass().getMethod("withdraw", UUID.class, double.class, defaultCurrency.getClass());
            method.invoke(gemsAPI, player.getUniqueId(), amount, defaultCurrency);
            return true;
        } catch (Exception e) {
            try {
                Method method = gemsAPI.getClass().getMethod("withdraw", UUID.class, double.class);
                method.invoke(gemsAPI, player.getUniqueId(), amount);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    @Override
    public boolean deposit(Player player, double amount) {
        try {
            Method method = gemsAPI.getClass().getMethod("deposit", UUID.class, double.class, defaultCurrency.getClass());
            method.invoke(gemsAPI, player.getUniqueId(), amount, defaultCurrency);
            return true;
        } catch (Exception e) {
            try {
                Method method = gemsAPI.getClass().getMethod("deposit", UUID.class, double.class);
                method.invoke(gemsAPI, player.getUniqueId(), amount);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    @Override
    public double getBalance(Player player) {
        try {
            Method method = gemsAPI.getClass().getMethod("getBalance", UUID.class, defaultCurrency.getClass());
            return (double) method.invoke(gemsAPI, player.getUniqueId(), defaultCurrency);
        } catch (Exception e) {
            try {
                Method method = gemsAPI.getClass().getMethod("getBalance", UUID.class);
                return (double) method.invoke(gemsAPI, player.getUniqueId());
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    @Override
    public String format(double amount) {
        try {
            Method method = defaultCurrency.getClass().getMethod("format", double.class);
            return (String) method.invoke(defaultCurrency, amount);
        } catch (Exception e) {
            return String.format("%.2f", amount);
        }
    }

    @Override
    public String getCurrencyName() {
        try {
            Method method = defaultCurrency.getClass().getMethod("getName");
            return (String) method.invoke(defaultCurrency);
        } catch (Exception e) {
            return "金币";
        }
    }
}
