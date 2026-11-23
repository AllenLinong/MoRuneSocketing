package cn.superiormc.enchantmentslots.methods;

import cn.superiormc.enchantmentslots.managers.ConfigManager;
import org.bukkit.inventory.ItemStack;

public final class EnchantsUtil {
    public static int getUsedSlot(ItemStack item) {
        return ConfigManager.configManager.usedSlots.getOrDefault(item, 1);
    }
}
