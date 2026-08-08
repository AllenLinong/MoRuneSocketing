package com.morunesocketing.integration;

import com.morunesocketing.MoRuneSocketing;
import com.morunesocketing.RuneManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class CustomFishingIntegration {

    private static final String PROVIDER_ID = "MoRuneSocketing";

    private final MoRuneSocketing plugin;
    private Object integrationManager;
    private boolean registered;

    public CustomFishingIntegration(MoRuneSocketing plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!plugin.getConfig().getBoolean("integrations.custom-fishing.enabled", true)) {
            return;
        }

        Plugin customFishing = Bukkit.getPluginManager().getPlugin("CustomFishing");
        if (customFishing == null || !customFishing.isEnabled()) {
            return;
        }

        try {
            ClassLoader classLoader = customFishing.getClass().getClassLoader();
            Class<?> apiClass = Class.forName(
                "net.momirealms.customfishing.api.BukkitCustomFishingPlugin",
                true,
                classLoader
            );
            Class<?> itemProviderClass = Class.forName(
                "net.momirealms.customfishing.api.integration.ItemProvider",
                true,
                classLoader
            );

            Object api = apiClass.getMethod("getInstance").invoke(null);
            integrationManager = apiClass.getMethod("getIntegrationManager").invoke(api);
            Object provider = Proxy.newProxyInstance(
                classLoader,
                new Class<?>[]{itemProviderClass},
                (proxy, method, args) -> switch (method.getName()) {
                    case "identifier" -> PROVIDER_ID;
                    case "buildItem" -> buildItem((String) args[1]);
                    case "itemID" -> RuneManager.getExternalItemId((ItemStack) args[0]);
                    case "toString" -> PROVIDER_ID + "ItemProvider";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                }
            );

            Method register = integrationManager.getClass().getMethod("registerItemProvider", itemProviderClass);
            registered = Boolean.TRUE.equals(register.invoke(integrationManager, provider));
            if (registered) {
                plugin.getLogger().info("CustomFishing integration enabled: " + PROVIDER_ID);
            } else {
                plugin.getLogger().warning("CustomFishing already has item provider: " + PROVIDER_ID);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("Failed to enable CustomFishing integration: " + exception.getMessage());
        }
    }

    public void disable() {
        if (!registered || integrationManager == null) {
            return;
        }

        try {
            integrationManager.getClass()
                .getMethod("unregisterItemProvider", String.class)
                .invoke(integrationManager, PROVIDER_ID);
        } catch (ReflectiveOperationException ignored) {
        } finally {
            registered = false;
            integrationManager = null;
        }
    }

    private ItemStack buildItem(String id) {
        if (id == null) {
            return new ItemStack(Material.AIR);
        }
        if (id.equalsIgnoreCase("remover") || id.equalsIgnoreCase("rune-remover")) {
            return RuneManager.createRuneRemoverItem(1);
        }
        if (id.regionMatches(true, 0, "rune/", 0, 5)) {
            ItemStack rune = RuneManager.createRuneItem(id.substring(5), 1);
            return rune != null ? rune : new ItemStack(Material.AIR);
        }
        ItemStack rune = RuneManager.createRuneItem(id, 1);
        return rune != null ? rune : new ItemStack(Material.AIR);
    }
}
