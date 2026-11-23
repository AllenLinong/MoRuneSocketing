package com.morunesocketing.integration;

import cn.superiormc.enchantmentslots.managers.ConfigManager;
import cn.superiormc.enchantmentslots.methods.SlotUtil;
import cn.superiormc.enchantmentslots.methods.EnchantsUtil;
import com.morunesocketing.MoRuneSocketing;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.morunesocketing.integration.EnchantmentSlotsIntegration.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Runs initialize() and real reflection lookups/invocations, without mocking SlotApi. */
class EnchantmentSlotsReflectionTest {
    private final MoRuneSocketing plugin = mock(MoRuneSocketing.class);
    private final Player player = mock(Player.class);
    private final ItemStack original = mock(ItemStack.class);
    private final ItemStack result = mock(ItemStack.class);
    private final Map<NamespacedKey, Object> originalData = new HashMap<>();
    private final Map<NamespacedKey, Object> resultData = new HashMap<>();
    private EnchantmentSlotsIntegration integration;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ConfigManager.configManager = new ConfigManager();
        Server server = mock(Server.class);
        PluginManager manager = mock(PluginManager.class);
        Plugin es = mock(Plugin.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(server.getPluginManager()).thenReturn(manager);
        when(manager.getPlugin("EnchantmentSlots")).thenReturn(es);
        when(es.isEnabled()).thenReturn(true);
        // The original has no enchants; the prepared result has a new enchantment.
        when(original.getEnchantments()).thenReturn(Map.of());
        Map<Enchantment, Integer> enchanted = mock(Map.class);
        when(enchanted.size()).thenReturn(1);
        when(result.getEnchantments()).thenReturn(enchanted);
        ItemMeta plainMeta = metaWithData(originalData);
        ItemMeta enchantedMeta = metaWithData(resultData);
        when(original.getItemMeta()).thenReturn(plainMeta);
        when(result.getItemMeta()).thenReturn(enchantedMeta);
        integration = new EnchantmentSlotsIntegration(plugin);
        integration.initialize();
    }

    private ItemMeta metaWithData(Map<NamespacedKey, Object> data) {
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        when(meta.getItemFlags()).thenReturn(Set.of());
        when(pdc.get(any(), eq(PersistentDataType.INTEGER))).thenAnswer(invocation -> data.get(invocation.getArgument(0)));
        when(pdc.has(any(), eq(PersistentDataType.INTEGER))).thenAnswer(invocation -> data.containsKey(invocation.getArgument(0)));
        doAnswer(invocation -> {
            data.put(invocation.getArgument(0), invocation.getArgument(2));
            return null;
        }).when(pdc).set(any(), eq(PersistentDataType.INTEGER), anyInt());
        return meta;
    }

    @Test
    void firstEnchantmentWorksWithoutOptionalEsMethods() {
        var check = integration.check(player, original, result);
        assertEquals(ALLOWED, check.status());
        assertEquals(3, originalData.get(SlotUtil.ENCHANTMENT_SLOTS_KEY));
        assertEquals(3, resultData.get(SlotUtil.ENCHANTMENT_SLOTS_KEY));
        assertEquals(1, ConfigManager.configManager.defaultLookups);
    }

    @Test
    void existingSlotLimitIsPreservedAndWeightedResultIsRejected() {
        originalData.put(SlotUtil.ENCHANTMENT_SLOTS_KEY, 1);
        resultData.put(SlotUtil.ENCHANTMENT_SLOTS_KEY, 1);
        ConfigManager.configManager.usedSlots.put(result, 2);
        var check = integration.check(player, original, result);
        assertEquals(LIMIT_REACHED, check.status());
        assertEquals(2, check.usedSlots());
        assertEquals(1, check.totalSlots());
        assertEquals(0, ConfigManager.configManager.defaultLookups);
    }

    @Test
    void extraSlotCapacityComesFromEsGetter() {
        originalData.put(SlotUtil.ENCHANTMENT_SLOTS_KEY, 1);
        ConfigManager.configManager.extraSlots.put(original, 2);
        ConfigManager.configManager.usedSlots.put(result, 3);
        var check = integration.check(player, original, result);
        assertEquals(ALLOWED, check.status());
        assertEquals(3, check.totalSlots());
    }

    @Test
    void rereadsConfigManagerAfterEsReload() {
        ConfigManager.configManager = new ConfigManager();
        ConfigManager.configManager.defaultSlots = 5;
        var check = integration.check(player, original, result);
        assertEquals(ALLOWED, check.status());
        assertEquals(5, check.totalSlots());
        assertEquals(5, resultData.get(SlotUtil.ENCHANTMENT_SLOTS_KEY));
    }

    @Test
    void itemsWithoutAMatchingSlotRuleRemainUnmanaged() {
        ConfigManager.configManager.defaultSlots = 0;
        assertEquals(ALLOWED, integration.check(player, original, result).status());
        assertFalse(originalData.containsKey(SlotUtil.ENCHANTMENT_SLOTS_KEY));
    }

    @Test
    void noCopyToCallIsNeededForPlainSwordSlotInitialization() {
        assertEquals(ALLOWED, integration.check(player, original, result).status());
        verify(original.getItemMeta().getPersistentDataContainer(), never()).copyTo(any(), anyBoolean());
    }

    @Test
    void supportsPlayerSetterWithoutRequiringTheIntegerOverload() throws Exception {
        var api = new EnchantmentSlotsIntegration.ReflectiveSlotApi(
                PlayerSetterSlots.class, EnchantsUtil.class, ConfigManager.class);
        api.initializeSlots(original, player);
        assertEquals(3, api.getSlot(original));
        assertTrue(api.hasBaseSlot(original));
        assertEquals(1, ConfigManager.configManager.defaultLookups);
    }

    @Test
    void missingConfigManagerBlocksInsteadOfSilentlyBypassingLimit() {
        ConfigManager.configManager = null;
        assertEquals(UNAVAILABLE, integration.check(player, original, result).status());
    }

    @Test
    void rejectedFirstEnchantmentDoesNotRerollCapacityOnRetry() {
        ConfigManager.configManager.usedSlots.put(result, 4);
        assertEquals(LIMIT_REACHED, integration.check(player, original, result).status());
        ConfigManager.configManager.defaultSlots = 5;
        assertEquals(LIMIT_REACHED, integration.check(player, original, result).status());
        assertEquals(1, ConfigManager.configManager.defaultLookups);
        assertEquals(3, originalData.get(SlotUtil.ENCHANTMENT_SLOTS_KEY));
    }

    public static final class PlayerSetterSlots {
        public static ItemStack setSlot(ItemStack item, Player player, boolean override) {
            return SlotUtil.setSlot(item, ConfigManager.configManager.getDefaultLimits(item, player), override);
        }

        public static int getSlot(ItemStack item) {
            return SlotUtil.getSlot(item);
        }
    }
}
