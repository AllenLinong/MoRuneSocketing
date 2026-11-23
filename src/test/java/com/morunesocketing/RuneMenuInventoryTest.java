package com.morunesocketing;

import com.morunesocketing.menu.RuneMenuHolder;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Bukkit events against stateful inventories exercise both sides of a transfer. */
class RuneMenuInventoryTest {
    @TempDir Path folder;
    private final MoRuneSocketing plugin = mock(MoRuneSocketing.class);
    private final Player player = mock(Player.class);
    private final PlayerInventory backpack = mock(PlayerInventory.class);
    private final Inventory top = mock(Inventory.class);
    private final InventoryView view = mock(InventoryView.class);
    private final YamlConfiguration layout = new YamlConfiguration();
    private final DataManager data = mock(DataManager.class);
    private final ItemStack[] slots = new ItemStack[27];
    private final ItemStack[] playerSlots = new ItemStack[36];
    private final List<ItemStack> returned = new ArrayList<>();
    private final RuneListener listener = new RuneListener();
    private RuneMenuHolder holder;
    private MockedStatic<MoRuneSocketing> singleton;

    @BeforeEach
    void setUp() throws Exception {
        singleton = mockStatic(MoRuneSocketing.class);
        singleton.when(MoRuneSocketing::getInstance).thenReturn(plugin);
        RuneManager.clearAllCaches();
        when(plugin.getName()).thenReturn("MoRuneSocketing");
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        when(plugin.getDataManager()).thenReturn(data);
        when(plugin.getCurrentSuccessRate()).thenReturn(75);
        var config = new YamlConfiguration();
        config.set("socketable-items", List.of("DIAMOND_SWORD"));
        when(plugin.getConfig()).thenReturn(config);
        Files.writeString(folder.resolve("runes.yml"), "speed:\n  name: Speed rune\n  material: PAPER\n");
        layout.set("Shape", List.of("XXXXXXXXX", "XEXXRXXIX", "XXXXXXXXX"));
        layout.set("Buttons.I.lore", List.of("Rate: %success-rate%"));
        holder = new RuneMenuHolder(layout, 10, 13, 16);
        holder.bind(top);
        when(top.getHolder()).thenReturn(holder);
        when(top.getSize()).thenReturn(27);
        when(top.getMaxStackSize()).thenReturn(64);
        when(top.getItem(anyInt())).thenAnswer(invocation -> slots[invocation.getArgument(0, Integer.class)]);
        doAnswer(invocation -> {
            slots[invocation.getArgument(0, Integer.class)] = invocation.getArgument(1);
            return null;
        }).when(top).setItem(anyInt(), nullable(ItemStack.class));
        when(player.getInventory()).thenReturn(backpack);
        when(player.getOpenInventory()).thenReturn(view);
        when(player.isOnline()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(view.getPlayer()).thenReturn(player);
        when(view.getTopInventory()).thenReturn(top);
        when(view.getBottomInventory()).thenReturn(backpack);
        when(view.getInventory(anyInt())).thenAnswer(invocation -> {
            int raw = invocation.getArgument(0);
            return raw < 0 ? null : raw < 27 ? top : backpack;
        });
        when(view.convertSlot(anyInt())).thenAnswer(invocation -> {
            int raw = invocation.getArgument(0);
            return raw < 27 ? raw : raw - 27;
        });
        when(view.getItem(anyInt())).thenAnswer(invocation -> {
            int raw = invocation.getArgument(0);
            return raw < 27 ? slots[raw] : playerSlots[raw - 27];
        });
        doAnswer(invocation -> {
            int raw = invocation.getArgument(0);
            if (raw < 27) slots[raw] = invocation.getArgument(1);
            else playerSlots[raw - 27] = invocation.getArgument(1);
            return null;
        }).when(view).setItem(anyInt(), nullable(ItemStack.class));
        when(backpack.addItem(any(ItemStack.class))).thenAnswer(invocation -> {
            returned.add(invocation.getArgument(0));
            return new HashMap<Integer, ItemStack>();
        });
    }

    @AfterEach
    void tearDown() {
        RuneManager.clearAllCaches();
        singleton.close();
    }

    private ItemStack item(Material material, int amount) {
        return item(material, amount, null);
    }

    private ItemStack item(Material material, int amount, ItemMeta meta) {
        ItemStack item = mock(ItemStack.class);
        AtomicInteger count = new AtomicInteger(amount);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenAnswer(invocation -> count.get());
        doAnswer(invocation -> { count.set(invocation.getArgument(0)); return null; }).when(item).setAmount(anyInt());
        when(item.getMaxStackSize()).thenReturn(material == Material.DIAMOND_SWORD ? 1 : 64);
        when(item.hasItemMeta()).thenReturn(meta != null);
        when(item.getItemMeta()).thenReturn(meta);
        when(item.clone()).thenAnswer(invocation -> item(material, count.get(), meta));
        when(item.isSimilar(any())).thenAnswer(invocation -> {
            ItemStack other = invocation.getArgument(0);
            return other != null && material == other.getType() && meta == other.getItemMeta();
        });
        return item;
    }

    private ItemMeta meta(String name) {
        ItemMeta meta = mock(ItemMeta.class);
        when(meta.hasDisplayName()).thenReturn(true);
        when(meta.getDisplayName()).thenReturn(name);
        when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        return meta;
    }

    private ItemStack decoration(int slot, Material material) {
        ItemStack item = item(material, 1, meta("Menu button"));
        // This mock is immutable, so use the same object for its snapshot equality.
        doReturn(item).when(item).clone();
        slots[slot] = item;
        holder.recordDecoration(slot, item);
        return item;
    }

    private InventoryClickEvent click(int raw, ClickType click, InventoryAction action) {
        return new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, raw, click, action);
    }

    private void close() {
        listener.onInventoryClose(new InventoryCloseEvent(view));
    }

    @Test
    void shiftSwordGoesToEquipmentSlotEvenWhenTopLeftIsEmpty() {
        playerSlots[0] = item(Material.DIAMOND_SWORD, 1);
        var event = click(27, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        listener.onInventoryClick(event);
        assertTrue(event.isCancelled());
        assertNull(slots[0]);
        assertNull(playerSlots[0]);
        assertEquals(Material.DIAMOND_SWORD, slots[10].getType());
        close();
        assertEquals(1, returned.size());
        assertEquals(Material.DIAMOND_SWORD, returned.getFirst().getType());
    }

    @Test
    void occupiedEquipmentSlotLeavesSwordInBackpack() {
        ItemStack first = item(Material.DIAMOND_SWORD, 1);
        ItemStack second = item(Material.DIAMOND_SWORD, 1);
        slots[10] = first;
        playerSlots[0] = second;
        listener.onInventoryClick(click(27, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
        assertSame(first, slots[10]);
        assertSame(second, playerSlots[0]);
        assertNull(slots[0]);
    }

    @Test
    void invalidShiftItemNeverEntersMenu() {
        ItemStack dirt = item(Material.DIRT, 64);
        playerSlots[0] = dirt;
        listener.onInventoryClick(click(27, ClickType.SHIFT_RIGHT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
        assertSame(dirt, playerSlots[0]);
        assertNull(slots[10]);
        assertNull(slots[13]);
    }

    @Test
    void shiftRuneMergesOnlyAvailableSpaceAndLeavesRemainderInBackpack() {
        ItemMeta meta = meta("Speed rune");
        slots[13] = item(Material.PAPER, 60, meta);
        playerSlots[0] = item(Material.PAPER, 10, meta);
        listener.onInventoryClick(click(27, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
        assertEquals(64, slots[13].getAmount());
        assertEquals(6, playerSlots[0].getAmount());
        assertNull(slots[0]);
    }

    @Test
    void shiftFromInputReturnsItemButNeverMovesDecorations() {
        slots[10] = item(Material.DIAMOND_SWORD, 1);
        ItemStack border = decoration(0, Material.BLACK_STAINED_GLASS_PANE);
        listener.onInventoryClick(click(10, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
        listener.onInventoryClick(click(0, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
        assertNull(slots[10]);
        assertSame(border, slots[0]);
        assertEquals(1, returned.size());
    }

    @Test
    void fullBackpackKeepsShiftedItemInInput() {
        ItemStack sword = item(Material.DIAMOND_SWORD, 1);
        slots[10] = sword;
        when(backpack.addItem(any(ItemStack.class))).thenReturn(new HashMap<>(Map.of(0, sword)));
        listener.onInventoryClick(click(10, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
        assertSame(sword, slots[10]);
    }

    @ParameterizedTest
    @EnumSource(value = ClickType.class, names = {"NUMBER_KEY", "SWAP_OFFHAND", "MIDDLE", "DOUBLE_CLICK"})
    void shortcutsCannotInsertUnvalidatedItems(ClickType shortcut) {
        var event = click(13, shortcut, shortcut == ClickType.DOUBLE_CLICK
                ? InventoryAction.COLLECT_TO_CURSOR : InventoryAction.HOTBAR_SWAP);
        listener.onInventoryClick(event);
        assertTrue(event.isCancelled());
        assertNull(slots[13]);
    }

    @Test
    void doubleClickFromBackpackCannotCollectMenuButtons() {
        var event = click(27, ClickType.DOUBLE_CLICK, InventoryAction.COLLECT_TO_CURSOR);
        listener.onInventoryClick(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void anotherPluginsCancellationIsPreserved() {
        playerSlots[0] = item(Material.DIAMOND_SWORD, 1);
        var event = click(27, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        event.setCancelled(true);
        listener.onInventoryClick(event);
        assertTrue(event.isCancelled());
        assertNotNull(playerSlots[0]);
        assertNull(slots[10]);
        RuneManager.handleInsertMenuClick(event);
        assertNull(slots[10]);
    }

    @Test
    void normalSwordPlacementAndPickupRemainAllowed() {
        doReturn(item(Material.DIAMOND_SWORD, 1)).when(view).getCursor();
        var place = click(10, ClickType.LEFT, InventoryAction.PLACE_ALL);
        listener.onInventoryClick(place);
        assertFalse(place.isCancelled());
        when(view.getCursor()).thenReturn(null);
        var pickup = click(10, ClickType.RIGHT, InventoryAction.PICKUP_HALF);
        listener.onInventoryClick(pickup);
        assertFalse(pickup.isCancelled());
    }

    @Test
    void normalWrongRuneAndBorderPlacementsAreRejected() {
        doReturn(item(Material.DIAMOND_SWORD, 1)).when(view).getCursor();
        var runeSlot = click(13, ClickType.LEFT, InventoryAction.PLACE_ALL);
        var borderSlot = click(0, ClickType.LEFT, InventoryAction.PLACE_ALL);
        listener.onInventoryClick(runeSlot);
        listener.onInventoryClick(borderSlot);
        assertTrue(runeSlot.isCancelled());
        assertTrue(borderSlot.isCancelled());
    }

    @Test
    void closeReturnsMisplacedItemsInAnySlotAndDoesNotDuplicateOnSecondClose() {
        decoration(1, Material.BLACK_STAINED_GLASS_PANE);
        slots[0] = item(Material.DIAMOND_SWORD, 1);
        slots[10] = item(Material.DIAMOND_SWORD, 1);
        slots[13] = item(Material.PAPER, 5);
        close();
        close();
        assertTrue(holder.isClosed());
        assertEquals(3, returned.size());
        for (ItemStack slot : slots) assertNull(slot);
    }

    @Test
    void closeDropsOverflowWhenBackpackIsFull() {
        ItemStack sword = item(Material.DIAMOND_SWORD, 1);
        slots[0] = sword;
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(backpack.addItem(any(ItemStack.class))).thenReturn(new HashMap<>(Map.of(0, sword)));
        close();
        assertNull(slots[0]);
        verify(world).dropItemNaturally(player.getLocation(), sword);
    }

    @Test
    void refreshOnlyChangesButtonAndRetainsInputsAndMisplacedItems() {
        slots[0] = item(Material.DIAMOND_SWORD, 1);
        slots[10] = item(Material.DIAMOND_SWORD, 1);
        slots[13] = item(Material.PAPER, 12);
        ItemStack[] before = slots.clone();
        ItemStack button = decoration(16, Material.ANVIL);
        when(data.getPlayerMenuRate(any())).thenReturn(75);
        RuneManager.refreshMenu(player);
        assertSame(before[0], slots[0]);
        assertSame(before[10], slots[10]);
        assertSame(before[13], slots[13]);
        verify(top, never()).clear();
        verify(button.getItemMeta()).setLore(List.of("Rate: 75"));
        close();
        assertEquals(3, returned.size());
    }

    @Test
    void refreshDoesNotOverwritePlayerItemInConfirmSlot() {
        decoration(16, Material.ANVIL);
        ItemStack sword = item(Material.DIAMOND_SWORD, 1);
        slots[16] = sword;
        RuneManager.refreshMenu(player);
        assertSame(sword, slots[16]);
        close();
        assertEquals(1, returned.size());
    }

    @Test
    void closeAndRefreshKeepOriginalLayoutAfterConfigurationReload() throws Exception {
        slots[10] = item(Material.DIAMOND_SWORD, 1);
        Files.writeString(folder.resolve("menu-config.yml"), "Shape: ['ERIIIIIII']\n");
        RuneManager.reloadMenuConfig();
        RuneManager.refreshMenu(player);
        assertNotNull(slots[10]);
        close();
        assertEquals(1, returned.size());
    }

    @Test
    void otherInventoryWithSimilarTitleIsNotTreatedAsRuneMenu() {
        when(top.getHolder()).thenReturn(null);
        when(view.getTitle()).thenReturn("镶嵌: Another plugin");
        var event = click(0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        listener.onInventoryClick(event);
        assertFalse(event.isCancelled());
        RuneManager.refreshMenu(player);
        verify(top, never()).setItem(anyInt(), any());
    }

    private InventoryDragEvent drag(ItemStack cursor, Set<Integer> affected) {
        Map<Integer, ItemStack> additions = new HashMap<>();
        for (int slot : affected) additions.put(slot, cursor);
        return new InventoryDragEvent(view, cursor, cursor, false, additions);
    }

    @Test
    void dragAcrossInputAndBorderIsRejectedAsAWhole() {
        var event = drag(item(Material.DIAMOND_SWORD, 1), Set.of(10, 0));
        listener.onInventoryDrag(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void validDragIntoInputAndBackpackRemainsAllowed() {
        var event = drag(item(Material.DIAMOND_SWORD, 1), Set.of(10, 27));
        listener.onInventoryDrag(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void wrongItemDraggedIntoRuneSlotIsRejected() {
        var event = drag(item(Material.DIAMOND_SWORD, 1), Set.of(13));
        listener.onInventoryDrag(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void dragOnlyWithinBackpackRemainsAllowed() {
        var event = drag(item(Material.DIRT, 8), Set.of(27, 28));
        listener.onInventoryDrag(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void customTitleAndNonAirInputMaterialsStillOpenAnEmptyFunctionalMenu() throws Exception {
        Files.writeString(folder.resolve("menu-config.yml"), """
                Title: Rune Forge
                Shape: [' E  R  I ']
                Buttons:
                  E:
                    material: STONE
                    is-equipment-slot: true
                  R:
                    material: STONE
                    is-rune-slot: true
                  I:
                    material: AIR
                    is-insert-button: true
                """);
        Server server = mock(Server.class);
        when(player.getServer()).thenReturn(server);
        when(server.createInventory(any(InventoryHolder.class), eq(9), eq("Rune Forge"))).thenAnswer(invocation -> {
            holder = invocation.getArgument(0);
            when(top.getHolder()).thenReturn(holder);
            when(top.getSize()).thenReturn(9);
            return top;
        });
        RuneManager.openInsertMenu(player);
        assertEquals(1, holder.equipmentSlot());
        assertEquals(4, holder.runeSlot());
        assertSame(top, holder.getInventory());
        assertNull(slots[1]);
        assertNull(slots[4]);
        verify(player).openInventory(top);
        verify(data).addOpenMenuPlayer(player);

        playerSlots[0] = item(Material.DIAMOND_SWORD, 1);
        // Use a mocked bottom click because this test's view fixture has 27 top slots.
        var click = mock(InventoryClickEvent.class);
        when(click.getWhoClicked()).thenReturn(player);
        when(click.getInventory()).thenReturn(top);
        when(click.getClickedInventory()).thenReturn(backpack);
        when(click.getRawSlot()).thenReturn(27);
        when(click.getCurrentItem()).thenReturn(playerSlots[0]);
        when(click.isShiftClick()).thenReturn(true);
        listener.onInventoryClick(click);
        assertNotNull(slots[1]);
        verify(click).setCurrentItem(null);
        close();
        assertEquals(1, returned.size());
    }

    @Test
    void invalidLayoutIsRejectedBeforeCreatingAnInventory() throws Exception {
        Files.writeString(folder.resolve("menu-config.yml"), "Title: Rune Forge\nShape: ['XXXXXXXXX']\n");
        LanguageManager language = mock(LanguageManager.class);
        when(plugin.getLanguageManager()).thenReturn(language);
        when(language.getPrefix()).thenReturn("");
        when(language.getMessage("menu.config-error")).thenReturn("Invalid layout");
        RuneManager.openInsertMenu(player);
        verify(player).sendMessage("Invalid layout");
        verify(player, never()).openInventory(any(Inventory.class));
    }
}
