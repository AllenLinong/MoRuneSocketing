package com.morunesocketing.integration;

import com.morunesocketing.MoRuneSocketing;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

/** Optional bridge; all slot rules remain owned by EnchantmentSlots. */
public final class EnchantmentSlotsIntegration {
    private static final NamespacedKey BASE_SLOT_KEY =
            new NamespacedKey("enchantmentslots", "enchantment_slots");
    private final MoRuneSocketing plugin;
    private Plugin enchantmentSlots;
    private SlotApi api;
    private boolean errorLogged;

    public EnchantmentSlotsIntegration(MoRuneSocketing plugin) {
        this.plugin = plugin;
    }

    EnchantmentSlotsIntegration(MoRuneSocketing plugin, Plugin enchantmentSlots, SlotApi api) {
        this.plugin = plugin;
        this.enchantmentSlots = enchantmentSlots;
        this.api = api;
    }

    public void initialize() {
        enchantmentSlots = plugin.getServer().getPluginManager().getPlugin("EnchantmentSlots");
        api = null;
        errorLogged = false;
        if (enchantmentSlots == null || !enchantmentSlots.isEnabled()) {
            return;
        }
        try {
            api = new ReflectiveSlotApi(enchantmentSlots.getClass().getClassLoader());
            plugin.getLogger().info("EnchantmentSlots integration enabled.");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            logFailure("API initialization", exception);
        }
    }

    /**
     * Checks the prepared result before payment or rune consumption. Initializing
     * slots on the original item fixes a random default once, even on rejection.
     * The enchantments themselves are changed only on the result copy.
     */
    public CheckResult check(Player player, ItemStack original, ItemStack result) {
        // Buff runes do not change Bukkit enchantments and do not use ES slots.
        if (original.getEnchantments().equals(result.getEnchantments())
                || enchantmentSlots == null || !enchantmentSlots.isEnabled()) {
            return CheckResult.allowed();
        }
        if (api == null) {
            return new CheckResult(Status.UNAVAILABLE, 0, 0);
        }
        String stage = "default slot initialization";
        try {
            api.initializeSlots(original, player);
            ItemMeta originalMeta = original.getItemMeta();
            ItemMeta resultMeta = result.getItemMeta();
            stage = "copying initialized slot data";
            if (originalMeta == null || resultMeta == null) {
                throw new IllegalStateException("Equipment has no item metadata");
            }
            // Preserve the newly assigned base slots and ES's hidden-enchant flag.
            // Read the stable ES data key instead of requiring a new ES helper or
            // copying the entire container into the prepared item metadata.
            Integer baseSlots = originalMeta.getPersistentDataContainer().get(BASE_SLOT_KEY, PersistentDataType.INTEGER);
            if (baseSlots != null) {
                resultMeta.getPersistentDataContainer().set(BASE_SLOT_KEY, PersistentDataType.INTEGER, baseSlots);
            }
            resultMeta.addItemFlags(originalMeta.getItemFlags().toArray(ItemFlag[]::new));
            result.setItemMeta(resultMeta);

            // Some ignore rules depend on slot NBT, so check after initialization.
            stage = "checking ignored items";
            if (api.isIgnored(original)) {
                return CheckResult.allowed();
            }
            stage = "reading total slots";
            int limit = api.getSlot(original);
            if (limit == 0 && !api.hasBaseSlot(original)) {
                // No matching slot setting or stored limit: ES does not manage it.
                return CheckResult.allowed();
            }
            stage = "calculating enchantment slot cost";
            int used = api.getUsedSlot(result);
            return new CheckResult(used <= limit ? Status.ALLOWED : Status.LIMIT_REACHED, used, limit);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            logFailure(stage, exception);
            return new CheckResult(Status.UNAVAILABLE, 0, 0);
        }
    }

    private void logFailure(String stage, Throwable exception) {
        if (!errorLogged) {
            errorLogged = true;
            while (exception instanceof InvocationTargetException wrapped && wrapped.getCause() != null) {
                exception = wrapped.getCause();
            }
            plugin.getLogger().log(Level.SEVERE,
                    "Cannot check EnchantmentSlots limits during " + stage
                            + "; enchantment runes will be blocked until the integration recovers.",
                    exception);
        }
    }

    public enum Status { ALLOWED, LIMIT_REACHED, UNAVAILABLE }

    public record CheckResult(Status status, int usedSlots, int totalSlots) {
        public static CheckResult allowed() {
            return new CheckResult(Status.ALLOWED, 0, 0);
        }
    }

    interface SlotApi {
        void initializeSlots(ItemStack item, Player player) throws ReflectiveOperationException;
        boolean isIgnored(ItemStack item) throws ReflectiveOperationException;
        boolean hasBaseSlot(ItemStack item) throws ReflectiveOperationException;
        int getSlot(ItemStack item) throws ReflectiveOperationException;
        int getUsedSlot(ItemStack item) throws ReflectiveOperationException;
    }

    static final class ReflectiveSlotApi implements SlotApi {
        private static final String PACKAGE = "cn.superiormc.enchantmentslots.";
        private final Method setSlotForPlayer;
        private final Method setSlotByValue;
        private final Method getDefaultLimits;
        private final Method getSlot;
        private final Method getUsedSlot;
        private final Field configManager;
        private final Method isIgnore;

        ReflectiveSlotApi(ClassLoader loader) throws ReflectiveOperationException {
            this(Class.forName(PACKAGE + "methods.SlotUtil", true, loader),
                    Class.forName(PACKAGE + "methods.EnchantsUtil", true, loader),
                    Class.forName(PACKAGE + "managers.ConfigManager", true, loader));
        }

        ReflectiveSlotApi(Class<?> slots, Class<?> enchants, Class<?> config) throws ReflectiveOperationException {
            setSlotForPlayer = findMethod(slots, "setSlot", ItemStack.class, Player.class, boolean.class);
            setSlotByValue = setSlotForPlayer == null
                    ? slots.getMethod("setSlot", ItemStack.class, int.class, boolean.class) : null;
            getDefaultLimits = setSlotForPlayer == null
                    ? config.getMethod("getDefaultLimits", ItemStack.class, Player.class) : null;
            getSlot = slots.getMethod("getSlot", ItemStack.class);
            getUsedSlot = enchants.getMethod("getUsedSlot", ItemStack.class);
            configManager = config.getField("configManager");
            isIgnore = config.getMethod("isIgnore", ItemStack.class);
        }

        @Override
        public void initializeSlots(ItemStack item, Player player) throws ReflectiveOperationException {
            // Existing capacity must not be rerolled or replaced by defaults.
            if (hasBaseSlot(item)) {
                return;
            }
            if (setSlotForPlayer != null) {
                setSlotForPlayer.invoke(null, item, player, false);
            } else {
                int defaults = (int) getDefaultLimits.invoke(currentConfigManager(), item, player);
                setSlotByValue.invoke(null, item, defaults, false);
            }
        }

        @Override
        public boolean isIgnored(ItemStack item) throws ReflectiveOperationException {
            // Resolve the current manager each time so an ES reload is respected.
            return (boolean) isIgnore.invoke(currentConfigManager(), item);
        }

        @Override
        public boolean hasBaseSlot(ItemStack item) throws ReflectiveOperationException {
            ItemMeta meta = item.getItemMeta();
            return meta != null && meta.getPersistentDataContainer().has(BASE_SLOT_KEY, PersistentDataType.INTEGER);
        }

        @Override
        public int getSlot(ItemStack item) throws ReflectiveOperationException {
            return (int) getSlot.invoke(null, item);
        }

        @Override
        public int getUsedSlot(ItemStack item) throws ReflectiveOperationException {
            return (int) getUsedSlot.invoke(null, item);
        }

        private Object currentConfigManager() throws ReflectiveOperationException {
            Object manager = configManager.get(null);
            if (manager == null) {
                throw new IllegalStateException("EnchantmentSlots configuration manager is not initialized");
            }
            return manager;
        }

        private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
            try {
                return type.getMethod(name, parameters);
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        }
    }
}
