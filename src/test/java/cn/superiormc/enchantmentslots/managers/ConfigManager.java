package cn.superiormc.enchantmentslots.managers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/** Contract fixture for ES versions whose SlotUtil has only the integer setter. */
public final class ConfigManager {
    public static ConfigManager configManager = new ConfigManager();
    public int defaultSlots = 3;
    public int defaultLookups;
    public boolean ignore;
    public final Map<ItemStack, Integer> usedSlots = new IdentityHashMap<>();
    public final Map<ItemStack, Integer> extraSlots = new IdentityHashMap<>();

    public int getDefaultLimits(ItemStack item, Player player) {
        defaultLookups++;
        return defaultSlots;
    }

    public boolean isIgnore(ItemStack item) {
        return ignore;
    }
}
