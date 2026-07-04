package com.morunesocketing.economy;

import com.morunesocketing.MoRuneSocketing;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault 经济支持
 * 支持几乎所有经济插件（EssentialsX, iConomy, etc.）
 */
public class VaultProvider implements EconomyProvider {
    
    private MoRuneSocketing plugin;
    private Object economy;
    private boolean enabled = false;
    
    public VaultProvider(MoRuneSocketing plugin) {
        this.plugin = plugin;
        
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        
        try {
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(
                Class.forName("net.milkbowl.vault.economy.Economy")
            );
            if (rsp == null) {
                return;
            }
            
            economy = rsp.getProvider();
            enabled = true;
        } catch (Exception e) {
            plugin.getLogger().warning("Vault 加载失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getName() {
        return "Vault";
    }
    
    @Override
    public boolean isEnabled() {
        return enabled && economy != null;
    }
    
    @Override
    public boolean has(Player player, double amount) {
        try {
            return (boolean) economy.getClass()
                    .getMethod("has", OfflinePlayer.class, double.class)
                    .invoke(economy, (OfflinePlayer) player, amount);
        } catch (Exception e) {
            try {
                return (boolean) economy.getClass()
                        .getMethod("has", String.class, double.class)
                        .invoke(economy, player.getName(), amount);
            } catch (Exception ex) {
                return false;
            }
        }
    }
    
    @Override
    public boolean withdraw(Player player, double amount) {
        try {
            Object response = economy.getClass()
                    .getMethod("withdrawPlayer", OfflinePlayer.class, double.class)
                    .invoke(economy, (OfflinePlayer) player, amount);
            return (boolean) response.getClass().getMethod("transactionSuccess").invoke(response);
        } catch (Exception e) {
            try {
                Object response = economy.getClass()
                        .getMethod("withdrawPlayer", String.class, double.class)
                        .invoke(economy, player.getName(), amount);
                return (boolean) response.getClass().getMethod("transactionSuccess").invoke(response);
            } catch (Exception ex) {
                return false;
            }
        }
    }
    
    @Override
    public boolean deposit(Player player, double amount) {
        try {
            Object response = economy.getClass()
                    .getMethod("depositPlayer", OfflinePlayer.class, double.class)
                    .invoke(economy, (OfflinePlayer) player, amount);
            return (boolean) response.getClass().getMethod("transactionSuccess").invoke(response);
        } catch (Exception e) {
            try {
                Object response = economy.getClass()
                        .getMethod("depositPlayer", String.class, double.class)
                        .invoke(economy, player.getName(), amount);
                return (boolean) response.getClass().getMethod("transactionSuccess").invoke(response);
            } catch (Exception ex) {
                return false;
            }
        }
    }
    
    @Override
    public double getBalance(Player player) {
        try {
            return (double) economy.getClass()
                    .getMethod("getBalance", OfflinePlayer.class)
                    .invoke(economy, (OfflinePlayer) player);
        } catch (Exception e) {
            try {
                return (double) economy.getClass()
                        .getMethod("getBalance", String.class)
                        .invoke(economy, player.getName());
            } catch (Exception ex) {
                return 0;
            }
        }
    }
    
    @Override
    public String format(double amount) {
        try {
            return (String) economy.getClass()
                    .getMethod("format", double.class)
                    .invoke(economy, amount);
        } catch (Exception e) {
            return String.format("%.2f", amount);
        }
    }
    
    @Override
    public String getCurrencyName() {
        try {
            return (String) economy.getClass()
                    .getMethod("currencyNamePlural")
                    .invoke(economy);
        } catch (Exception e) {
            return "金币";
        }
    }
}
