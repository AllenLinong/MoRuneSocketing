package cn.superiormc.enchantmentslots.methods;

import cn.superiormc.enchantmentslots.managers.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Deliberately has no player setter or hasBaseSlot helper. */
public final class SlotUtil {
    public static final NamespacedKey ENCHANTMENT_SLOTS_KEY =
            new NamespacedKey("enchantmentslots", "enchantment_slots");

    public static ItemStack setSlot(ItemStack item, int slots, boolean override) {
        ItemMeta meta = item.getItemMeta();
        if (override || !meta.getPersistentDataContainer().has(ENCHANTMENT_SLOTS_KEY, PersistentDataType.INTEGER)) {
            if (slots > 0) {
                meta.getPersistentDataContainer().set(ENCHANTMENT_SLOTS_KEY, PersistentDataType.INTEGER, slots);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    public static int getSlot(ItemStack item) {
        Integer base = item.getItemMeta().getPersistentDataContainer()
                .get(ENCHANTMENT_SLOTS_KEY, PersistentDataType.INTEGER);
        return (base == null ? 0 : base) + ConfigManager.configManager.extraSlots.getOrDefault(item, 0);
    }
}
