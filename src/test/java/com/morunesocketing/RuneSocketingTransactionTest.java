package com.morunesocketing;

import com.morunesocketing.economy.EconomyManager;
import com.morunesocketing.integration.EnchantmentSlotsIntegration;
import com.morunesocketing.menu.RuneMenuHolder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.morunesocketing.integration.EnchantmentSlotsIntegration.Status.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises payment and inventory side effects through the real menu handler. */
class RuneSocketingTransactionTest {
    @TempDir Path folder;
    private final MoRuneSocketing plugin = mock(MoRuneSocketing.class);
    private final EconomyManager economy = mock(EconomyManager.class);
    private final EnchantmentSlotsIntegration integration = mock(EnchantmentSlotsIntegration.class);
    private final Player player = mock(Player.class);
    private final Inventory menu = mock(Inventory.class);
    private final InventoryClickEvent click = mock(InventoryClickEvent.class);
    private final ItemStack original = mock(ItemStack.class);
    private final ItemStack result = mock(ItemStack.class);
    private final ItemStack rune = mock(ItemStack.class);
    private final YamlConfiguration config = new YamlConfiguration();
    private MockedStatic<MoRuneSocketing> singleton;

    @BeforeEach
    void setUp() throws Exception {
        singleton = mockStatic(MoRuneSocketing.class);
        singleton.when(MoRuneSocketing::getInstance).thenReturn(plugin);
        RuneManager.clearAllCaches();
        // A buff rune exercises the common transaction without a live server's
        // enchantment registry; the slot bridge decision is supplied separately.
        Files.writeString(folder.resolve("runes.yml"), """
                speed:
                  name: Speed rune
                  material: PAPER
                  buff: custom_speed
                  buff-level: 1
                """);
        Files.writeString(folder.resolve("menu-config.yml"), "Title: 镶嵌\n");
        config.set("socketable-items", java.util.List.of("DIAMOND_SWORD"));
        config.set("features.play-sound", false);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        when(plugin.getName()).thenReturn("MoRuneSocketing");
        when(plugin.getCurrentSuccessRate()).thenReturn(100);
        when(plugin.getEnchantmentSlotsIntegration()).thenReturn(integration);
        when(integration.check(player, original, result)).thenReturn(EnchantmentSlotsIntegration.CheckResult.allowed());
        when(plugin.getEconomyManager()).thenReturn(economy);
        when(economy.isEnabled()).thenReturn(true);
        when(economy.getInsertCost()).thenReturn(1000.0);
        when(economy.hasEnough(player, 1000.0)).thenReturn(true);
        when(economy.withdraw(player, 1000.0)).thenReturn(true);
        LanguageManager language = mock(LanguageManager.class);
        when(plugin.getLanguageManager()).thenReturn(language);
        when(language.getPrefix()).thenReturn("");
        when(language.getMessage(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(language.getMessage(anyString(), any(String[].class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(click.getWhoClicked()).thenReturn(player);
        when(click.getInventory()).thenReturn(menu);
        when(click.getRawSlot()).thenReturn(16);
        when(menu.getSize()).thenReturn(27);
        var holder = new RuneMenuHolder(new YamlConfiguration(), 10, 13, 16);
        holder.bind(menu);
        when(menu.getHolder()).thenReturn(holder);
        when(click.getClick()).thenReturn(ClickType.LEFT);
        when(menu.getItem(10)).thenReturn(original);
        when(menu.getItem(13)).thenReturn(rune);
        when(original.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(rune.getType()).thenReturn(Material.PAPER);
        when(rune.getAmount()).thenReturn(1);
        when(original.clone()).thenReturn(result);
        mockMeta(original, null);
        mockMeta(result, null);
        ItemMeta runeMeta = mockMeta(rune, "Speed rune");
        when(runeMeta.getPersistentDataContainer().get(new NamespacedKey(plugin, "rune_type"), PersistentDataType.STRING))
                .thenReturn("speed");
    }

    private ItemMeta mockMeta(ItemStack item, String name) {
        ItemMeta meta = mock(ItemMeta.class);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        when(meta.hasDisplayName()).thenReturn(name != null);
        when(meta.getDisplayName()).thenReturn(name);
        return meta;
    }

    @AfterEach
    void tearDown() {
        RuneManager.clearAllCaches();
        singleton.close();
    }

    @Test
    void slotRejectionDoesNotChargeOrConsumeItems() {
        when(integration.check(player, original, result)).thenReturn(
                new EnchantmentSlotsIntegration.CheckResult(LIMIT_REACHED, 4, 3));
        RuneManager.handleInsertMenuClick(click);
        verify(integration).check(player, original, result);
        verifyNoInteractions(economy);
        verify(menu, never()).setItem(anyInt(), any());
        verify(original, never()).setItemMeta(any());
    }

    @Test
    void bridgeFailureDoesNotChargeOrConsumeItems() {
        when(integration.check(player, original, result)).thenReturn(
                new EnchantmentSlotsIntegration.CheckResult(UNAVAILABLE, 0, 0));
        RuneManager.handleInsertMenuClick(click);
        verifyNoInteractions(economy);
        verify(menu, never()).setItem(anyInt(), any());
    }

    @Test
    void failedUpfrontPaymentPreservesEquipmentAndRune() {
        when(economy.withdraw(player, 1000.0)).thenReturn(false);
        RuneManager.handleInsertMenuClick(click);
        verify(economy).withdraw(player, 1000.0);
        verify(menu, never()).setItem(anyInt(), any());
        verify(original, never()).setItemMeta(any());
    }

    @Test
    void failedSuccessOnlyPaymentPreservesEquipmentAndRune() {
        config.set("socketing.economy.deduct-on-success-only", true);
        when(economy.withdraw(player, 1000.0)).thenReturn(false);
        RuneManager.handleInsertMenuClick(click);
        verify(economy).withdraw(player, 1000.0);
        verify(menu, never()).setItem(anyInt(), any());
        verify(original, never()).setItemMeta(any());
    }

    @Test
    void successCommitsPreparedResultAfterValidationAndPayment() {
        RuneManager.handleInsertMenuClick(click);
        var order = inOrder(integration, economy, menu);
        order.verify(integration).check(player, original, result);
        order.verify(economy).withdraw(player, 1000.0);
        order.verify(menu).setItem(13, null);
        order.verify(menu).setItem(10, result);
        verify(original, never()).setItemMeta(any());
    }

    @Test
    void failedRollKeepsOriginalAndHonorsRuneConsumptionSetting() {
        when(plugin.getCurrentSuccessRate()).thenReturn(0);
        config.set("socketing.economy.deduct-on-success-only", true);
        config.set("features.consume-rune-on-fail", false);
        RuneManager.handleInsertMenuClick(click);
        verify(economy, never()).withdraw(any(), anyDouble());
        verify(menu, never()).setItem(anyInt(), any());
        verify(original, never()).setItemMeta(any());
    }
}
