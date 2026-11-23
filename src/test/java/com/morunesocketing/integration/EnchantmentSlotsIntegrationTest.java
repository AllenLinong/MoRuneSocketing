package com.morunesocketing.integration;

import com.morunesocketing.MoRuneSocketing;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import static com.morunesocketing.integration.EnchantmentSlotsIntegration.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnchantmentSlotsIntegrationTest {
    private final MoRuneSocketing plugin = mock(MoRuneSocketing.class);
    private final Plugin slotsPlugin = mock(Plugin.class);
    private final EnchantmentSlotsIntegration.SlotApi api = mock(EnchantmentSlotsIntegration.SlotApi.class);
    private final Player player = mock(Player.class);
    private final ItemStack original = mock(ItemStack.class);
    private final ItemStack result = mock(ItemStack.class);
    private final ItemMeta originalMeta = mock(ItemMeta.class);
    private final ItemMeta resultMeta = mock(ItemMeta.class);
    private final PersistentDataContainer originalData = mock(PersistentDataContainer.class);
    private final PersistentDataContainer resultData = mock(PersistentDataContainer.class);
    private final Logger logger = mock(Logger.class);
    private EnchantmentSlotsIntegration integration;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        when(plugin.getLogger()).thenReturn(logger);
        when(slotsPlugin.isEnabled()).thenReturn(true);
        // ES owns enchantment parsing and weights. Distinct maps represent an
        // enchantment change without bootstrapping a Minecraft registry here.
        Map<Enchantment, Integer> before = mock(Map.class);
        Map<Enchantment, Integer> after = mock(Map.class);
        when(original.getEnchantments()).thenReturn(before);
        when(result.getEnchantments()).thenReturn(after);
        when(original.getItemMeta()).thenReturn(originalMeta);
        when(result.getItemMeta()).thenReturn(resultMeta);
        when(originalMeta.getPersistentDataContainer()).thenReturn(originalData);
        when(resultMeta.getPersistentDataContainer()).thenReturn(resultData);
        when(originalMeta.getItemFlags()).thenReturn(Set.of(ItemFlag.HIDE_ENCHANTS));
        when(api.hasBaseSlot(original)).thenReturn(true);
        when(api.getSlot(original)).thenReturn(3);
        integration = new EnchantmentSlotsIntegration(plugin, slotsPlugin, api);
    }

    @Test
    void rejectsNewEnchantmentBeyondCapacity() throws Exception {
        when(api.getUsedSlot(result)).thenReturn(4);
        var check = integration.check(player, original, result);
        assertEquals(LIMIT_REACHED, check.status());
        assertEquals(4, check.usedSlots());
        assertEquals(3, check.totalSlots());
        verify(original, never()).setItemMeta(any());
    }

    @Test
    void allowsExistingEnchantmentUpgradeAtFullCapacity() throws Exception {
        when(api.getUsedSlot(result)).thenReturn(3);
        assertEquals(ALLOWED, integration.check(player, original, result).status());
    }

    @Test
    void honorsWeightedEnchantmentCostInsteadOfCountingKinds() throws Exception {
        when(api.getUsedSlot(result)).thenReturn(5);
        var check = integration.check(player, original, result);
        assertEquals(LIMIT_REACHED, check.status());
        assertEquals(5, check.usedSlots());
    }

    @Test
    void includesExtraSlotsReturnedByEs() throws Exception {
        when(api.getSlot(original)).thenReturn(5);
        when(api.getUsedSlot(result)).thenReturn(5);
        assertEquals(ALLOWED, integration.check(player, original, result).status());
    }

    @Test
    void checksIgnoredItemsAfterInitializingSlotData() throws Exception {
        when(api.isIgnored(original)).thenReturn(true);
        assertEquals(ALLOWED, integration.check(player, original, result).status());
        var order = inOrder(api);
        order.verify(api).initializeSlots(original, player);
        order.verify(api).isIgnored(original);
        verify(api, never()).getUsedSlot(any());
    }

    @Test
    void allowsItemsWithoutAnyApplicableLimit() throws Exception {
        when(api.getSlot(original)).thenReturn(0);
        when(api.hasBaseSlot(original)).thenReturn(false);
        assertEquals(ALLOWED, integration.check(player, original, result).status());
        verify(api, never()).getUsedSlot(any());
    }

    @Test
    void honorsAnExplicitlyStoredZeroLimit() throws Exception {
        when(api.getSlot(original)).thenReturn(0);
        when(api.getUsedSlot(result)).thenReturn(1);
        assertEquals(LIMIT_REACHED, integration.check(player, original, result).status());
    }

    @Test
    void preservesInitializedSlotDataAndHideFlagsInPreparedResult() throws Exception {
        NamespacedKey key = new NamespacedKey("enchantmentslots", "enchantment_slots");
        when(originalData.get(key, PersistentDataType.INTEGER)).thenReturn(3);
        integration.check(player, original, result);
        verify(api, times(1)).initializeSlots(original, player);
        verify(resultData).set(key, PersistentDataType.INTEGER, 3);
        verify(originalData, never()).copyTo(any(), anyBoolean());
        verify(resultMeta).addItemFlags(ItemFlag.HIDE_ENCHANTS);
        verify(result).setItemMeta(resultMeta);
    }

    @Test
    void buffRunesSkipSlotChecksEvenWhenBridgeIsUnavailable() {
        Map<Enchantment, Integer> unchanged = original.getEnchantments();
        when(result.getEnchantments()).thenReturn(unchanged);
        integration = new EnchantmentSlotsIntegration(plugin, slotsPlugin, null);
        assertEquals(ALLOWED, integration.check(player, original, result).status());
        verifyNoInteractions(api);
    }

    @Test
    void worksWithoutEnchantmentSlotsInstalled() {
        integration = new EnchantmentSlotsIntegration(plugin, null, null);
        assertEquals(ALLOWED, integration.check(player, original, result).status());
        verifyNoInteractions(api);
    }

    @Test
    void skipsDisabledEnchantmentSlots() {
        when(slotsPlugin.isEnabled()).thenReturn(false);
        assertEquals(ALLOWED, integration.check(player, original, result).status());
        verifyNoInteractions(api);
    }

    @Test
    void refusesEnchantmentChangesWhenInstalledApiIsUnavailable() {
        integration = new EnchantmentSlotsIntegration(plugin, slotsPlugin, null);
        assertEquals(UNAVAILABLE, integration.check(player, original, result).status());
    }

    @Test
    void refusesEnchantmentChangesWhenEsThrows() throws Exception {
        IllegalStateException cause = new IllegalStateException("ES reloading");
        doThrow(new InvocationTargetException(cause))
                .when(api).initializeSlots(original, player);
        assertEquals(UNAVAILABLE, integration.check(player, original, result).status());
        verify(result, never()).setItemMeta(any());
        verify(logger).log(eq(Level.SEVERE), contains("default slot initialization"), same(cause));
    }

    @Test
    void blocksQueryFailuresAndLogsOnlyOnce() throws Exception {
        when(api.getUsedSlot(result)).thenThrow(new IllegalStateException("Slot configuration unavailable"));
        assertEquals(UNAVAILABLE, integration.check(player, original, result).status());
        assertEquals(UNAVAILABLE, integration.check(player, original, result).status());
        verify(logger, times(1)).log(eq(Level.SEVERE), anyString(), any(Throwable.class));
    }

    @Test
    void retriesAfterATransientEsFailure() throws Exception {
        when(api.getUsedSlot(result)).thenThrow(new IllegalStateException("ES reloading")).thenReturn(3);
        assertEquals(UNAVAILABLE, integration.check(player, original, result).status());
        assertEquals(ALLOWED, integration.check(player, original, result).status());
    }
}
