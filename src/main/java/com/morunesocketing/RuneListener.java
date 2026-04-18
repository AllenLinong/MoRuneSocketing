package com.morunesocketing;

import com.morunesocketing.util.SchedulerUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RuneListener implements Listener {
    
    // ==================== 常量定义 ====================
    
    private static final String NBT_RUNE_TYPE = "rune_type";
    private static final String NBT_NO_CRAFT = "no_craft";
    private static final String NBT_CUSTOM_BUFF = "custom_buff";
    private static final String BUFF_TYPE_LUCK = "LUCK";
    private static final String MENU_TITLE_KEYWORD = "镶嵌";
    
    // ==================== 配置字段 ====================
    
    private static long BUFF_CHECK_PERIOD_TICKS = 100L;
    private static long RENEW_THRESHOLD_TICKS = 200L;
    private static final long BUFF_CHECK_DELAY_TICKS = 10L;
    private static final int BUFF_DURATION_TICKS = 30 * 20;
    private static boolean NBT_CACHE_ENABLED = true;
    private static int NBT_CACHE_SIZE = 10000;
    
    // ==================== 槽位配置 ====================
    
    private static boolean CHECK_MAIN_HAND = true;
    private static boolean CHECK_OFF_HAND = true;
    private static boolean CHECK_HELMET = true;
    private static boolean CHECK_CHESTPLATE = true;
    private static boolean CHECK_LEGGINGS = true;
    private static boolean CHECK_BOOTS = true;
    
    // ==================== BUFF类型映射 ====================
    
    private static final Map<String, String> BUFF_TYPE_MAPPING = new HashMap<>();
    
    static {
        BUFF_TYPE_MAPPING.put("custom_luck", "LUCK");
        BUFF_TYPE_MAPPING.put("custom_speed", "SPEED");
        BUFF_TYPE_MAPPING.put("custom_haste", "FAST_DIGGING");
        BUFF_TYPE_MAPPING.put("custom_resistance", "DAMAGE_RESISTANCE");
        BUFF_TYPE_MAPPING.put("custom_fire_resistance", "FIRE_RESISTANCE");
        BUFF_TYPE_MAPPING.put("custom_water_breathing", "WATER_BREATHING");
        BUFF_TYPE_MAPPING.put("custom_invisibility", "INVISIBILITY");
        BUFF_TYPE_MAPPING.put("custom_night_vision", "NIGHT_VISION");
        BUFF_TYPE_MAPPING.put("custom_health_boost", "HEALTH_BOOST");
        BUFF_TYPE_MAPPING.put("custom_regeneration", "REGENERATION");
        BUFF_TYPE_MAPPING.put("custom_absorption", "ABSORPTION");
        BUFF_TYPE_MAPPING.put("custom_saturation", "SATURATION");
        BUFF_TYPE_MAPPING.put("custom_jump", "JUMP");
        BUFF_TYPE_MAPPING.put("custom_strength", "INCREASE_DAMAGE");
        BUFF_TYPE_MAPPING.put("custom_slow_falling", "SLOW_FALLING");
        BUFF_TYPE_MAPPING.put("custom_dolphins_grace", "DOLPHINS_GRACE");
        BUFF_TYPE_MAPPING.put("custom_conduit_power", "CONDUIT_POWER");
        BUFF_TYPE_MAPPING.put("custom_hero_of_the_village", "HERO_OF_THE_VILLAGE");
        BUFF_TYPE_MAPPING.put("custom_bad_luck", "UNLUCK");
        BUFF_TYPE_MAPPING.put("custom_slow", "SLOW");
        BUFF_TYPE_MAPPING.put("custom_mining_fatigue", "MINING_FATIGUE");
        BUFF_TYPE_MAPPING.put("custom_nausea", "CONFUSION");
        BUFF_TYPE_MAPPING.put("custom_blindness", "BLINDNESS");
        BUFF_TYPE_MAPPING.put("custom_hunger", "HUNGER");
        BUFF_TYPE_MAPPING.put("custom_weakness", "WEAKNESS");
        BUFF_TYPE_MAPPING.put("custom_poison", "POISON");
        BUFF_TYPE_MAPPING.put("custom_wither", "WITHER");
        BUFF_TYPE_MAPPING.put("custom_levitation", "LEVITATION");
        BUFF_TYPE_MAPPING.put("custom_glowing", "GLOWING");
    }
    
    // ==================== 缓存字段 ====================
    
    private final Map<String, String> playerEquipmentCache = new HashMap<>();
    private final Map<String, Set<PotionEffectType>> playerAppliedBuffs = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> itemBuffCache = new LinkedHashMap<UUID, Map<String, Integer>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Map<String, Integer>> eldest) {
            return size() > NBT_CACHE_SIZE;
        }
    };
    
    // ==================== 配置加载 ====================
    
    public static void loadConfig() {
        ConfigurationSection config = MoRuneSocketing.getInstance().getConfig().getConfigurationSection("buff-check");
        if (config != null) {
            BUFF_CHECK_PERIOD_TICKS = config.getLong("check-period-ticks", 100L);
            RENEW_THRESHOLD_TICKS = config.getLong("renew-threshold-ticks", 200L);
            CHECK_MAIN_HAND = config.getBoolean("check-main-hand", true);
            CHECK_OFF_HAND = config.getBoolean("check-off-hand", true);
            CHECK_HELMET = config.getBoolean("check-helmet", true);
            CHECK_CHESTPLATE = config.getBoolean("check-chestplate", true);
            CHECK_LEGGINGS = config.getBoolean("check-leggings", true);
            CHECK_BOOTS = config.getBoolean("check-boots", true);
            NBT_CACHE_ENABLED = config.getBoolean("nbt-cache-enabled", true);
            NBT_CACHE_SIZE = config.getInt("nbt-cache-size", 10000);
        }
    }
    
    // ==================== 初始化 ====================
    
    public void startBuffCheckTask() {
        SchedulerUtils.runTaskTimer(this::asyncCheckAllPlayers, BUFF_CHECK_DELAY_TICKS, BUFF_CHECK_PERIOD_TICKS);
    }
    
    // ==================== 异步检查 ====================
    
    private void asyncCheckAllPlayers() {
        // 在主线程中获取在线玩家列表并检查BUFF状态，避免异步线程安全问题
        SchedulerUtils.runTask(() -> {
            List<Player> onlinePlayers = new ArrayList<>(org.bukkit.Bukkit.getOnlinePlayers());
            List<Player> playersToUpdate = new ArrayList<>();
            
            // 在主线程中检查所有玩家
            for (Player player : onlinePlayers) {
                // 检查装备变化或BUFF是否需要续期
                if (hasEquipmentChanged(player) || shouldRenewBuffs(player)) {
                    playersToUpdate.add(player);
                }
            }
            
            if (!playersToUpdate.isEmpty()) {
                // 在主线程中应用BUFF更新
                for (Player player : playersToUpdate) {
                    if (player.isOnline()) {
                        checkAndApplyBuffs(player);
                    }
                }
            }
        });
    }
    
    private boolean needsBuffUpdate(Player player) {
        return hasEquipmentChanged(player) || shouldRenewBuffs(player);
    }
    
    private boolean shouldRenewBuffs(Player player) {
        String playerId = player.getUniqueId().toString();
        Set<PotionEffectType> appliedTypes = playerAppliedBuffs.get(playerId);
        
        if (appliedTypes == null || appliedTypes.isEmpty()) {
            return false;
        }
        
        // 获取玩家当前所有激活的BUFF类型
        Set<PotionEffectType> currentActiveTypes = new HashSet<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            currentActiveTypes.add(effect.getType());
            // 检查已应用的BUFF是否需要续期（剩余时间小于阈值）
            if (appliedTypes.contains(effect.getType())) {
                if (effect.getDuration() < RENEW_THRESHOLD_TICKS) {
                    return true;
                }
            }
        }
        
        // 检查是否有已应用的BUFF已经完全消失（需要重新应用）
        for (PotionEffectType appliedType : appliedTypes) {
            if (!currentActiveTypes.contains(appliedType)) {
                // BUFF已经完全消失，需要重新应用
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasEquipmentChanged(Player player) {
        String playerId = player.getUniqueId().toString();
        String currentEquipment = getEquipmentHash(player);
        String cachedEquipment = playerEquipmentCache.get(playerId);
        
        if (cachedEquipment == null || !cachedEquipment.equals(currentEquipment)) {
            playerEquipmentCache.put(playerId, currentEquipment);
            return true;
        }
        
        return false;
    }
    
    private String getEquipmentHash(Player player) {
        StringBuilder hash = new StringBuilder();
        
        hash.append(getItemTypeString(CHECK_MAIN_HAND ? player.getInventory().getItemInMainHand() : null)).append("-");
        hash.append(getItemTypeString(CHECK_OFF_HAND ? player.getInventory().getItemInOffHand() : null)).append("-");
        
        ItemStack[] armor = player.getInventory().getArmorContents();
        hash.append(getItemTypeString(CHECK_HELMET ? armor[0] : null)).append("-");
        hash.append(getItemTypeString(CHECK_CHESTPLATE ? armor[1] : null)).append("-");
        hash.append(getItemTypeString(CHECK_LEGGINGS ? armor[2] : null)).append("-");
        hash.append(getItemTypeString(CHECK_BOOTS ? armor[3] : null));
        
        return hash.toString();
    }
    
    private String getItemTypeString(ItemStack item) {
        return item != null ? item.getType().toString() : "AIR";
    }
    
    // ==================== 装备变化事件 ====================
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        if (title.contains(MENU_TITLE_KEYWORD)) {
            handleMenuClick(event);
            return;
        }
        
        if (isEquipmentSlot(event)) {
            scheduleBuffUpdate(player);
        }
    }
    
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (CHECK_MAIN_HAND || CHECK_OFF_HAND) {
            scheduleBuffUpdate(event.getPlayer());
        }
    }
    
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (CHECK_MAIN_HAND) {
            scheduleBuffUpdate(event.getPlayer());
        }
    }
    
    private void scheduleBuffUpdate(Player player) {
        SchedulerUtils.runTaskForEntity(player, () -> {
            if (player.isOnline()) {
                checkAndApplyBuffs(player);
            }
        });
    }
    
    private boolean isEquipmentSlot(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return false;
        }
        
        int slot = event.getSlot();
        InventoryType.SlotType slotType = event.getSlotType();
        
        return slotType == InventoryType.SlotType.ARMOR || slot == 45 || slot == 40;
    }
    
    // ==================== 菜单事件 ====================
    
    private void handleMenuClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        int rawSlot = event.getRawSlot();
        
        if (rawSlot >= 0 && rawSlot < inventory.getSize()) {
            RuneManager.handleInsertMenuClick(event);
        } else {
            event.setCancelled(false);
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        if (!title.contains(MENU_TITLE_KEYWORD)) {
            return;
        }
        
        Inventory inventory = event.getInventory();
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < inventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        if (title.contains(MENU_TITLE_KEYWORD)) {
            RuneManager.handleMenuClose(event);
        }
    }
    
    // ==================== 玩家事件 ====================
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        checkAndApplyBuffs(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerId = player.getUniqueId().toString();
        
        Set<PotionEffectType> appliedTypes = playerAppliedBuffs.get(playerId);
        if (appliedTypes != null) {
            for (PotionEffectType type : appliedTypes) {
                if (player.hasPotionEffect(type)) {
                    player.removePotionEffect(type);
                }
            }
        }
        
        playerEquipmentCache.remove(playerId);
        playerAppliedBuffs.remove(playerId);
    }
    
    // ==================== 交互事件 ====================
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item != null && isRuneItem(item)) {
            event.setCancelled(true);
        }
        
        if (isRuneHeadPlacement(event, item)) {
            event.setCancelled(true);
        }
    }
    
    private boolean isRuneHeadPlacement(PlayerInteractEvent event, ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return false;
        }
        
        if (!RuneManager.isRune(item)) {
            return false;
        }
        
        return event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }
    
    // ==================== 合成事件 ====================
    
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (shouldPreventCrafting(item)) {
                event.getInventory().setResult(null);
                break;
            }
        }
    }
    
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (shouldPreventCrafting(item)) {
                event.setCancelled(true);
                break;
            }
        }
    }
    
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        
        if (shouldPreventCrafting(item)) {
            event.setCancelled(true);
        }
    }
    
    private boolean shouldPreventCrafting(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return isRuneItem(item) || hasNoCraftTag(meta) || com.morunesocketing.RuneManager.isRune(item);
    }
    
    private boolean isRuneItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(
            new NamespacedKey(MoRuneSocketing.getInstance(), NBT_RUNE_TYPE),
            PersistentDataType.STRING
        );
    }
    
    private boolean hasNoCraftTag(ItemMeta meta) {
        if (meta == null) {
            return false;
        }
        
        return meta.getPersistentDataContainer().has(
            new NamespacedKey(MoRuneSocketing.getInstance(), NBT_NO_CRAFT),
            PersistentDataType.BOOLEAN
        );
    }
    
    // ==================== BUFF管理 ====================
    
    private void checkAndApplyBuffs(Player player) {
        String playerId = player.getUniqueId().toString();
        Set<PotionEffectType> appliedEffectTypes = playerAppliedBuffs.computeIfAbsent(
            playerId, k -> new HashSet<>()
        );
        Set<PotionEffectType> previousAppliedTypes = new HashSet<>(appliedEffectTypes);
        appliedEffectTypes.clear();
        
        if (CHECK_MAIN_HAND) {
            applyBuffsFromItem(player, player.getInventory().getItemInMainHand(), appliedEffectTypes);
        }
        if (CHECK_OFF_HAND) {
            applyBuffsFromItem(player, player.getInventory().getItemInOffHand(), appliedEffectTypes);
        }
        
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (CHECK_HELMET) {
            applyBuffsFromItem(player, armor[0], appliedEffectTypes);
        }
        if (CHECK_CHESTPLATE) {
            applyBuffsFromItem(player, armor[1], appliedEffectTypes);
        }
        if (CHECK_LEGGINGS) {
            applyBuffsFromItem(player, armor[2], appliedEffectTypes);
        }
        if (CHECK_BOOTS) {
            applyBuffsFromItem(player, armor[3], appliedEffectTypes);
        }
        
        // 只移除之前由插件应用但现在不再应用的buff
        for (PotionEffect effect : player.getActivePotionEffects()) {
            PotionEffectType type = effect.getType();
            if (previousAppliedTypes.contains(type) && !appliedEffectTypes.contains(type)) {
                player.removePotionEffect(type);
            }
        }
    }
    
    private void applyBuffsFromItem(Player player, ItemStack item, Set<PotionEffectType> appliedEffectTypes) {
        if (item == null) {
            return;
        }
        
        if (RuneManager.isRune(item)) {
            return;
        }
        
        applyLegacyLuckBuff(player, item, appliedEffectTypes);
        
        if (item.hasItemMeta()) {
            applyCustomBuff(player, item, item.getItemMeta(), appliedEffectTypes);
        }
    }
    
    private void applyLegacyLuckBuff(Player player, ItemStack item, Set<PotionEffectType> appliedEffectTypes) {
        if (!RuneManager.hasBuffEffect(item, BUFF_TYPE_LUCK)) {
            return;
        }
        
        int level = RuneManager.getBuffLevel(item, BUFF_TYPE_LUCK);
        if (level > 0) {
            applyPotionEffect(player, PotionEffectType.LUCK, level - 1, appliedEffectTypes);
        }
    }
    
    private void applyCustomBuff(Player player, ItemStack item, ItemMeta meta, Set<PotionEffectType> appliedEffectTypes) {
        Map<String, Integer> buffsMap = getCachedBuffData(item, meta);
        
        if (buffsMap == null || buffsMap.isEmpty()) {
            return;
        }
        
        if (!buffsMap.isEmpty()) {
            for (Map.Entry<String, Integer> entry : buffsMap.entrySet()) {
                String buffId = entry.getKey();
                int level = entry.getValue();
                applyBuffByType(player, buffId, level, appliedEffectTypes);
            }
        }
    }
    
    private Map<String, Integer> getCachedBuffData(ItemStack item, ItemMeta meta) {
        if (!NBT_CACHE_ENABLED) {
            return parseBuffDataFromItem(meta);
        }
        
        UUID itemId = getItemUUID(item);
        if (itemBuffCache.containsKey(itemId)) {
            return itemBuffCache.get(itemId);
        }
        
        Map<String, Integer> buffData = parseBuffDataFromItem(meta);
        itemBuffCache.put(itemId, buffData);
        return buffData;
    }
    
    private UUID getItemUUID(ItemStack item) {
        if (item == null) {
            return UUID.randomUUID();
        }
        
        // 使用更复杂的哈希算法避免冲突
        long hash1 = (long) item.getType().hashCode() << 32;
        long hash2 = 0L;
        
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            // 使用更稳定的哈希算法
            hash2 = (long) meta.hashCode() ^ (long) System.identityHashCode(meta);
        }
        
        // 添加时间戳和随机数进一步避免冲突
        long timestamp = System.nanoTime() & 0xFFFFL; // 取低16位
        long random = (long) (Math.random() * 0xFFFFL);
        
        return new UUID(hash1 | timestamp, hash2 | random);
    }
    
    private Map<String, Integer> parseBuffDataFromItem(ItemMeta meta) {
        String customBuffsData = meta.getPersistentDataContainer().get(
            new NamespacedKey(MoRuneSocketing.getInstance(), NBT_CUSTOM_BUFF),
            PersistentDataType.STRING
        );
        
        // 使用RuneManager中的现有方法，消除重复代码
        return com.morunesocketing.RuneManager.parseBuffsData(customBuffsData);
    }
    
    private int getBuffLevelFromLore(List<String> lore) {
        if (lore == null) {
            return 0;
        }
        
        for (String line : lore) {
            if (line.contains("级")) {
                return parseLevelFromLine(line);
            }
        }
        return 0;
    }
    
    private int parseLevelFromLine(String line) {
        try {
            String levelStr = line.replaceAll(".*: §f", "").replace("级", "").trim();
            return Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            if (line.contains("1级")) return 1;
            if (line.contains("2级")) return 2;
            if (line.contains("3级")) return 3;
            if (line.contains("4级")) return 4;
            if (line.contains("5级")) return 5;
            return 0;
        }
    }
    
    private void applyBuffByType(Player player, String buffType, int level, Set<PotionEffectType> appliedEffectTypes) {
        PotionEffectType effectType = getPotionEffectType(buffType);
        
        if (effectType != null) {
            applyPotionEffect(player, effectType, level - 1, appliedEffectTypes);
        }
    }
    
    private PotionEffectType getPotionEffectType(String buffType) {
        String effectName = BUFF_TYPE_MAPPING.get(buffType);
        
        if (effectName != null) {
            return PotionEffectType.getByName(effectName);
        }
        
        if (buffType.startsWith("custom_")) {
            String potentialEffect = buffType.substring(7).toUpperCase();
            return PotionEffectType.getByName(potentialEffect);
        }
        
        return PotionEffectType.getByName(buffType.toUpperCase());
    }
    
    private void applyPotionEffect(Player player, PotionEffectType type, int amplifier, Set<PotionEffectType> appliedEffectTypes) {
        if (type == null) {
            return;
        }
        player.addPotionEffect(new PotionEffect(type, BUFF_DURATION_TICKS, amplifier, true, false));
        appliedEffectTypes.add(type);
    }
}
