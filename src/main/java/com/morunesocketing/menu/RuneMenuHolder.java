package com.morunesocketing.menu;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/** Owns the layout and decorations of one open menu, including across reloads. */
public final class RuneMenuHolder implements InventoryHolder {
    private final ConfigurationSection config;
    private final int equipmentSlot;
    private final int runeSlot;
    private final int insertSlot;
    private final Map<Integer, ItemStack> decorations = new HashMap<>();
    private Inventory inventory;
    private boolean closed;

    public RuneMenuHolder(ConfigurationSection config, int equipmentSlot, int runeSlot, int insertSlot) {
        this.config = config;
        this.equipmentSlot = equipmentSlot;
        this.runeSlot = runeSlot;
        this.insertSlot = insertSlot;
    }

    public void bind(Inventory inventory) {
        if (this.inventory != null) {
            throw new IllegalStateException("Menu inventory already assigned");
        }
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Menu inventory not assigned yet");
        }
        return inventory;
    }

    public ConfigurationSection config() { return config; }
    public int equipmentSlot() { return equipmentSlot; }
    public int runeSlot() { return runeSlot; }
    public int insertSlot() { return insertSlot; }
    public boolean isInput(int slot) { return slot == equipmentSlot || slot == runeSlot; }
    public boolean isClosed() { return closed; }
    public void close() { closed = true; }

    public void recordDecoration(int slot, ItemStack item) {
        if (!isInput(slot) && item != null && item.getType() != Material.AIR) {
            decorations.put(slot, item.clone());
        }
    }

    public boolean isDecoration(int slot, ItemStack item) {
        ItemStack expected = decorations.get(slot);
        return expected != null && item != null && expected.equals(item);
    }
}
