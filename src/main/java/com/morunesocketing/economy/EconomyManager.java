package com.morunesocketing.economy;

import com.morunesocketing.MoRuneSocketing;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * 经济管理器
 * 支持多种经济插件：Vault, PlayerPoints, GemsEconomy, RedisEconomy, PEconomy
 */
public class EconomyManager {
    
    private static EconomyManager instance;
    private MoRuneSocketing plugin;
    private EconomyProvider provider;
    private boolean enabled = false;
    
    public EconomyManager(MoRuneSocketing plugin) {
        this.plugin = plugin;
        instance = this;
    }
    
    public static EconomyManager getInstance() {
        return instance;
    }
    
    /**
     * 初始化经济系统
     */
    public boolean initialize() {
        if (!plugin.getConfig().getBoolean("socketing.economy.enabled", false)) {
            return false;
        }
        
        String configuredProvider = plugin.getConfig().getString("socketing.economy.provider", "");
        
        // 如果配置了特定提供商，优先尝试
        if (!configuredProvider.isEmpty()) {
            provider = createProvider(configuredProvider);
            if (provider != null && provider.isEnabled()) {
                enabled = true;
                plugin.getLogger().info("经济系统已启用 (提供商: " + provider.getName() + ")");
                return true;
            }
        }
        
        // 自动检测已安装的经济插件（按优先级）
        String[] providers = {"Vault", "PlayerPoints", "GemsEconomy", "RedisEconomy", "PEconomy"};
        
        for (String providerName : providers) {
            provider = createProvider(providerName);
            if (provider != null && provider.isEnabled()) {
                enabled = true;
                plugin.getLogger().info("经济系统已启用 (自动检测: " + provider.getName() + ")");
                return true;
            }
        }
        
        plugin.getLogger().warning("未找到可用的经济插件，经济功能将禁用");
        plugin.getLogger().warning("支持的经济插件: Vault, PlayerPoints, GemsEconomy, RedisEconomy, PEconomy");
        return false;
    }
    
    /**
     * 创建经济提供商实例
     */
    private EconomyProvider createProvider(String name) {
        switch (name.toLowerCase()) {
            case "vault":
                return new VaultProvider(plugin);
            case "playerpoints":
                return new PlayerPointsProvider(plugin);
            case "gemseconomy":
                return new GemsEconomyProvider(plugin);
            case "rediseconomy":
                return new RedisEconomyProvider(plugin);
            case "peconomy":
                return new PEconomyProvider(plugin);
            default:
                return null;
        }
    }
    
    /**
     * 检查玩家是否有足够的余额
     */
    public boolean hasEnough(Player player, double amount) {
        if (!enabled || provider == null) {
            return true; // 经济系统禁用时，默认允许
        }
        return provider.has(player, amount);
    }
    
    /**
     * 扣除玩家余额
     */
    public boolean withdraw(Player player, double amount) {
        if (!enabled || provider == null) {
            return true; // 经济系统禁用时，默认成功
        }
        return provider.withdraw(player, amount);
    }
    
    /**
     * 给玩家增加余额
     */
    public boolean deposit(Player player, double amount) {
        if (!enabled || provider == null) {
            return true;
        }
        return provider.deposit(player, amount);
    }
    
    /**
     * 获取玩家余额
     */
    public double getBalance(Player player) {
        if (!enabled || provider == null) {
            return 0;
        }
        return provider.getBalance(player);
    }
    
    /**
     * 获取镶嵌费用
     */
    public double getInsertCost() {
        return plugin.getConfig().getDouble("socketing.economy.cost", 1000);
    }
    
    /**
     * 格式化金额显示
     */
    public String format(double amount) {
        if (provider != null) {
            return provider.format(amount);
        }
        return String.format("%.2f", amount);
    }
    
    /**
     * 获取货币名称
     */
    public String getCurrencyName() {
        if (provider != null) {
            return provider.getCurrencyName();
        }
        return "金币";
    }
    
    /**
     * 检查经济系统是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 获取当前提供商名称
     */
    public String getProviderName() {
        return provider != null ? provider.getName() : "None";
    }
    
    /**
     * 关闭经济系统
     */
    public void shutdown() {
        provider = null;
        enabled = false;
        instance = null;
        plugin.getLogger().info("经济系统已关闭");
    }
}
