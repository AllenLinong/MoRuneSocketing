package com.morunesocketing;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 符文管理器
 * 负责符文配置管理、菜单系统、符文应用等核心功能
 */
public class RuneManager {
    
    // ==================== 常量定义 ====================
    
    // 配置文件名
    private static final String RUNES_CONFIG_FILE = "runes.yml";
    private static final String MENU_CONFIG_FILE = "menu-config.yml";
    
    // 缓存配置
    private static final long CONFIG_CACHE_DURATION_MS = 30000; // 30秒缓存
    
    // 菜单布局符号
    private static final char SYMBOL_EQUIPMENT_SLOT = 'E';
    private static final char SYMBOL_RUNE_SLOT = 'R';
    private static final char SYMBOL_INSERT_BUTTON = 'I';
    private static final char SYMBOL_EMPTY = ' ';
    
    // 菜单默认槽位（兼容旧版本）
    private static final int DEFAULT_EQUIPMENT_SLOT = 10;
    private static final int DEFAULT_RUNE_SLOT = 13;
    private static final int DEFAULT_INSERT_BUTTON_SLOT = 16;
    private static final int DEFAULT_MENU_SIZE = 27;
    private static final int MENU_COLUMNS = 9;
    private static final int MAX_MENU_ROWS = 6;
    
    // 物品堆叠限制
    private static final int MIN_ITEM_AMOUNT = 1;
    private static final int MAX_ITEM_AMOUNT = 64;
    
    // 附魔和BUFF等级限制
    private static final int DEFAULT_ENCHANT_MAX_LEVEL = 10;
    private static final int DEFAULT_BUFF_MAX_LEVEL = 5;
    
    // 颜色代码
    private static final String COLOR_SUCCESS = "§a";
    private static final String COLOR_ERROR = "§c";
    private static final String COLOR_INFO = "§7";
    private static final String COLOR_HIGHLIGHT = "§f";
    
    // 消息前缀
    private static final String DEFAULT_PREFIX = "§8[§6MoRune§8] §r";
    
    // NBT键名
    private static final String NBT_MENU_BUTTON = "menu_button";
    private static final String NBT_CUSTOM_ENCHANTMENT = "custom_enchantment";
    private static final String NBT_CUSTOM_BUFF = "custom_buff";
    private static final String NBT_BUFF_DISPLAY_MAP = "buff_display_map"; // 存储buffId到显示名称的映射
    private static final String NBT_RUNE_TYPE = "rune_type";
    private static final String NBT_NO_CRAFT = "no_craft";
    
    // 符文名称关键词
    private static final String KEYWORD_SHARPNESS = "锋利";
    private static final String KEYWORD_FORTUNE = "时运";
    private static final String KEYWORD_UNBREAKING = "耐久";
    private static final String KEYWORD_EFFICIENCY = "效率";
    private static final String KEYWORD_SMITE = "亡灵杀手";
    private static final String KEYWORD_BANE_OF_ARTHROPODS = "节肢杀手";
    private static final String KEYWORD_LOOTING = "抢夺";
    private static final String KEYWORD_LUCK = "幸运";
    private static final String KEYWORD_PROTECTION = "保护";
    
    // BUFF显示格式
    private static final String BUFF_DISPLAY_FORMAT = "§a幸运BUFF: §f%d级";
    
    // ==================== 缓存字段 ====================
    
    private static final Map<String, ConfigurationSection> runeConfigCache = new HashMap<>();
    private static final Map<String, Long> socketingCooldowns = new HashMap<>();
    private static ConfigurationSection allRunesConfigCache = null;
    private static ConfigurationSection menuConfigCache = null;
    private static long lastConfigLoadTime = 0;
    private static long lastMenuConfigLoadTime = 0;
    
    // BUFF数据解析缓存
    private static String lastParsedData = null;
    private static Map<String, Integer> lastParsedResult = null;
    
    // ==================== 配置管理 ====================
    
    /**
     * 获取消息前缀
     */
    private static String getPrefix() {
        return MoRuneSocketing.getInstance().getLanguageManager().getPrefix();
    }
    
    /**
     * 发送消息（带前缀）
     */
    private static void sendMessage(Player player, String message) {
        player.sendMessage(getPrefix() + message);
    }
    
    /**
     * 发送消息（带前缀，支持CommandSender）
     */
    private static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(getPrefix() + message);
    }
    
    /**
     * 获取菜单配置
     * 使用缓存机制减少文件IO操作
     * @return 菜单配置节，如果加载失败返回null
     */
    public static ConfigurationSection getMenuConfig() {
        if (isMenuCacheExpired()) {
            clearMenuConfigCache();
        }
        
        if (menuConfigCache != null) {
            return menuConfigCache;
        }
        
        return loadMenuConfigFromFile();
    }
    
    /**
     * 检查菜单缓存是否过期
     */
    private static boolean isMenuCacheExpired() {
        return System.currentTimeMillis() - lastMenuConfigLoadTime > CONFIG_CACHE_DURATION_MS;
    }
    
    /**
     * 从文件加载菜单配置
     */
    private static ConfigurationSection loadMenuConfigFromFile() {
        File menuFile = new File(MoRuneSocketing.getInstance().getDataFolder(), MENU_CONFIG_FILE);
        
        if (!menuFile.exists()) {
            return null;
        }
        
        try {
            YamlConfiguration menuConfig = YamlConfiguration.loadConfiguration(menuFile);
            menuConfigCache = menuConfig;
            lastMenuConfigLoadTime = System.currentTimeMillis();
            return menuConfigCache;
        } catch (Exception e) {
            MoRuneSocketing.getInstance().getLogger().warning("加载菜单配置文件失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 清除菜单配置缓存
     */
    public static void clearMenuConfigCache() {
        menuConfigCache = null;
        lastMenuConfigLoadTime = 0;
    }
    
    /**
     * 重载符文配置文件
     */
    public static void reloadRuneConfig() {
        clearRuneConfigCache();
    }
    
    /**
     * 重载菜单配置文件
     */
    public static void reloadMenuConfig() {
        clearMenuConfigCache();
    }
    
    /**
     * 获取指定符文的配置
     * @param runeId 符文ID
     * @return 符文配置节，如果不存在返回null
     */
    public static ConfigurationSection getRuneConfig(String runeId) {
        if (isRuneCacheExpired()) {
            clearRuneConfigCache();
        }
        
        String lowerRuneId = runeId.toLowerCase();
        
        if (runeConfigCache.containsKey(lowerRuneId)) {
            return runeConfigCache.get(lowerRuneId);
        }
        
        ConfigurationSection config = loadRuneConfigFromFile(runeId);
        if (config != null) {
            runeConfigCache.put(lowerRuneId, config);
        }
        return config;
    }
    
    /**
     * 检查符文缓存是否过期
     */
    private static boolean isRuneCacheExpired() {
        return System.currentTimeMillis() - lastConfigLoadTime > CONFIG_CACHE_DURATION_MS;
    }
    
    /**
     * 从文件加载符文配置
     */
    private static ConfigurationSection loadRuneConfigFromFile(String runeId) {
        // 优先从分离的符文配置文件加载
        File runesFile = new File(MoRuneSocketing.getInstance().getDataFolder(), RUNES_CONFIG_FILE);
        
        if (runesFile.exists()) {
            try {
                YamlConfiguration runesConfig = YamlConfiguration.loadConfiguration(runesFile);
                // 不区分大小写查找符文ID
                ConfigurationSection config = findConfigIgnoreCase(runesConfig, runeId);
                lastConfigLoadTime = System.currentTimeMillis();
                return config;
            } catch (Exception e) {
                // 加载失败，回退到主配置文件
            }
        }
        
        // 回退到主配置文件（兼容旧版）
        return MoRuneSocketing.getInstance().getConfig().getConfigurationSection("runes." + runeId);
    }
    
    /**
     * 不区分大小写查找配置
     */
    private static ConfigurationSection findConfigIgnoreCase(YamlConfiguration config, String key) {
        String lowerKey = key.toLowerCase();
        
        for (String configKey : config.getKeys(false)) {
            if (configKey.toLowerCase().equals(lowerKey)) {
                return config.getConfigurationSection(configKey);
            }
        }
        
        return null;
    }
    
    /**
     * 获取所有符文配置
     * @return 包含所有符文的配置节
     */
    public static ConfigurationSection getAllRunesConfig() {
        if (isRuneCacheExpired()) {
            clearRuneConfigCache();
        }
        
        if (allRunesConfigCache != null) {
            return allRunesConfigCache;
        }
        
        return loadAllRunesConfigFromFile();
    }
    
    /**
     * 从文件加载所有符文配置
     */
    private static ConfigurationSection loadAllRunesConfigFromFile() {
        File runesFile = new File(MoRuneSocketing.getInstance().getDataFolder(), RUNES_CONFIG_FILE);
        
        if (runesFile.exists()) {
            try {
                YamlConfiguration runesConfig = YamlConfiguration.loadConfiguration(runesFile);
                allRunesConfigCache = runesConfig;
                lastConfigLoadTime = System.currentTimeMillis();
                return allRunesConfigCache;
            } catch (Exception e) {
                // 加载失败，回退到主配置文件
            }
        }
        
        // 回退到主配置文件（兼容旧版）
        allRunesConfigCache = MoRuneSocketing.getInstance().getConfig().getConfigurationSection("runes");
        return allRunesConfigCache;
    }
    
    /**
     * 清除符文配置缓存
     */
    public static void clearRuneConfigCache() {
        runeConfigCache.clear();
        allRunesConfigCache = null;
        lastConfigLoadTime = 0;
    }
    
    /**
     * 清除所有配置缓存
     */
    public static void clearAllCaches() {
        clearRuneConfigCache();
        clearMenuConfigCache();
    }
    
    /**
     * 获取所有符文ID列表
     * @return 符文ID集合
     */
    public static Set<String> getAllRuneIds() {
        ConfigurationSection runesConfig = getAllRunesConfig();
        if (runesConfig == null) {
            return new java.util.HashSet<>();
        }
        return runesConfig.getKeys(false);
    }
    
    // ==================== 菜单系统 ====================
    
    /**
     * 打开符文镶嵌菜单
     * @param player 要打开菜单的玩家
     */
    public static void openInsertMenu(Player player) {
        if (!MoRuneSocketing.getInstance().getConfig().getBoolean("features.enable-socketing-menu", true)) {
            sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("menu.disabled"));
            return;
        }
        
        ConfigurationSection menuConfig = getMenuConfig();
        
        if (menuConfig == null) {
            sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("menu.config-error"));
            return;
        }
        
        // 缓存玩家打开菜单时的成功率（解决菜单显示不同步问题）
        int currentRate = MoRuneSocketing.getInstance().getCurrentSuccessRate();
        MoRuneSocketing.getInstance().getDataManager().cachePlayerMenuRate(player.getUniqueId(), currentRate);
        
        // 记录玩家打开菜单（用于实时刷新）
        MoRuneSocketing.getInstance().getDataManager().addOpenMenuPlayer(player);
        
        String menuTitle = menuConfig.getString("Title", "§8§l符文镶嵌系统");
        List<String> shape = menuConfig.getStringList("Shape");
        int menuSize = calculateMenuSize(shape);
        
        Inventory menu = player.getServer().createInventory(null, menuSize, menuTitle);
        setupInsertMenu(menu, menuConfig, player);
        
        player.openInventory(menu);
    }
    
    /**
     * 根据Shape布局计算菜单大小
     * @param shape Shape布局列表
     * @return 菜单大小（9的倍数）
     */
    private static int calculateMenuSize(List<String> shape) {
        if (shape == null || shape.isEmpty()) {
            return DEFAULT_MENU_SIZE;
        }
        
        int rows = Math.min(shape.size(), MAX_MENU_ROWS);
        int maxCols = MENU_COLUMNS;
        
        // 获取最大列数
        for (String row : shape) {
            maxCols = Math.max(maxCols, row.length());
        }
        
        // 确保列数至少为9
        maxCols = Math.max(maxCols, MENU_COLUMNS);
        
        return rows * MENU_COLUMNS;
    }
    
    /**
     * 根据Shape布局找到对应符号的槽位
     * @param menuConfig 菜单配置
     * @param targetSymbol 目标符号
     * @return 槽位索引，未找到返回-1
     */
    private static int findSlotBySymbol(ConfigurationSection menuConfig, char targetSymbol) {
        List<String> shape = menuConfig.getStringList("Shape");
        
        if (shape == null || shape.isEmpty()) {
            return getDefaultSlotForSymbol(targetSymbol);
        }
        
        int slot = 0;
        for (String row : shape) {
            for (int col = 0; col < row.length(); col++) {
                if (row.charAt(col) == targetSymbol) {
                    return slot;
                }
                slot++;
            }
            // 补齐到9列
            slot += (MENU_COLUMNS - row.length());
        }
        
        return -1;
    }
    
    /**
     * 根据属性查找槽位（支持自定义配置）
     * @param menuConfig 菜单配置
     * @param property 属性名称
     * @return 槽位索引，未找到返回-1
     */
    private static int findSlotByProperty(ConfigurationSection menuConfig, String property) {
        List<String> shape = menuConfig.getStringList("Shape");
        ConfigurationSection buttons = menuConfig.getConfigurationSection("Buttons");
        
        if (shape == null || shape.isEmpty() || buttons == null) {
            return -1;
        }
        
        int slot = 0;
        for (String row : shape) {
            for (int col = 0; col < row.length(); col++) {
                char symbol = row.charAt(col);
                String symbolStr = String.valueOf(symbol);
                ConfigurationSection buttonConfig = buttons.getConfigurationSection(symbolStr);
                
                if (buttonConfig != null && buttonConfig.getBoolean(property, false)) {
                    return slot;
                }
                slot++;
            }
            // 补齐到9列
            slot += (MENU_COLUMNS - row.length());
        }
        
        return -1;
    }
    
    /**
     * 查找装备槽位置
     */
    private static int findEquipmentSlot(ConfigurationSection menuConfig) {
        int slot = findSlotByProperty(menuConfig, "is-equipment-slot");
        return slot >= 0 ? slot : findSlotBySymbol(menuConfig, SYMBOL_EQUIPMENT_SLOT);
    }
    
    /**
     * 查找符文槽位置
     */
    private static int findRuneSlot(ConfigurationSection menuConfig) {
        int slot = findSlotByProperty(menuConfig, "is-rune-slot");
        return slot >= 0 ? slot : findSlotBySymbol(menuConfig, SYMBOL_RUNE_SLOT);
    }
    
    /**
     * 查找镶嵌按钮位置
     */
    private static int findInsertButtonSlot(ConfigurationSection menuConfig) {
        int slot = findSlotByProperty(menuConfig, "is-insert-button");
        return slot >= 0 ? slot : findSlotBySymbol(menuConfig, SYMBOL_INSERT_BUTTON);
    }
    
    /**
     * 获取默认槽位（兼容旧版本）
     */
    private static int getDefaultSlotForSymbol(char symbol) {
        return switch (symbol) {
            case SYMBOL_EQUIPMENT_SLOT -> DEFAULT_EQUIPMENT_SLOT;
            case SYMBOL_RUNE_SLOT -> DEFAULT_RUNE_SLOT;
            case SYMBOL_INSERT_BUTTON -> DEFAULT_INSERT_BUTTON_SLOT;
            default -> -1;
        };
    }
    
    /**
     * 设置镶嵌菜单布局
     * @param menu 菜单库存
     * @param menuConfig 菜单配置
     * @param player 打开菜单的玩家（用于显示正确的成功率）
     */
    private static void setupInsertMenu(Inventory menu, ConfigurationSection menuConfig, Player player) {
        menu.clear();
        
        List<String> shape = menuConfig.getStringList("Shape");
        if (shape == null || shape.isEmpty()) {
            setupDefaultMenu(menu);
            return;
        }
        
        ConfigurationSection buttons = menuConfig.getConfigurationSection("Buttons");
        if (buttons == null) {
            return;
        }
        
        parseShapeAndSetupButtons(menu, shape, buttons, menuConfig, player);
    }
    
    /**
     * 解析Shape布局并设置按钮
     */
    private static void parseShapeAndSetupButtons(Inventory menu, List<String> shape, ConfigurationSection buttons, ConfigurationSection menuConfig, Player player) {
        int slot = 0;
        
        for (String rowStr : shape) {
            // 处理当前行的每个字符
            for (int col = 0; col < rowStr.length(); col++) {
                char symbol = rowStr.charAt(col);
                
                if (symbol == SYMBOL_EMPTY) {
                    slot++;
                    continue;
                }
                
                String symbolStr = String.valueOf(symbol);
                ConfigurationSection buttonConfig = buttons.getConfigurationSection(symbolStr);
                
                if (buttonConfig != null) {
                    ItemStack button = createButton(buttonConfig, symbolStr, menuConfig, player);
                    if (button != null) {
                        menu.setItem(slot, button);
                    }
                }
                
                slot++;
            }
            
            // 补齐到9列（每行必须是9个槽位）
            if (rowStr.length() < MENU_COLUMNS) {
                slot += (MENU_COLUMNS - rowStr.length());
            }
        }
    }
    
    /**
     * 创建按钮物品
     * @param buttonConfig 按钮配置
     * @param symbol 按钮符号
     * @param menuConfig 菜单配置（用于变量替换）
     * @param player 打开菜单的玩家（用于显示正确的成功率）
     * @return 按钮物品
     */
    private static ItemStack createButton(ConfigurationSection buttonConfig, String symbol, ConfigurationSection menuConfig, Player player) {
        String materialName = buttonConfig.getString("material", "AIR");
        Material material = Material.getMaterial(materialName.toUpperCase());
        
        if (material == null || material == Material.AIR) {
            return null;
        }
        
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        
        // 设置名称
        String name = buttonConfig.getString("name");
        if (name != null) {
            meta.setDisplayName(ColorUtils.translate(name));
        }
        
        // 设置Lore（支持变量替换，使用玩家缓存的菜单成功率）
        List<String> lore = buttonConfig.getStringList("lore");
        if (!lore.isEmpty()) {
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(ColorUtils.translate(replaceVariables(line, menuConfig, player)));
            }
            meta.setLore(processedLore);
        }
        
        // 添加自定义标识
        meta.getPersistentDataContainer().set(
            new NamespacedKey(MoRuneSocketing.getInstance(), NBT_MENU_BUTTON),
            PersistentDataType.STRING,
            symbol
        );
        
        // 设置自定义模型数据
        if (buttonConfig.contains("custom-model-data")) {
            int customModelData = buttonConfig.getInt("custom-model-data", 0);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
        }
        
        // 添加附魔光效
        if (buttonConfig.getBoolean("glow", false)) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        button.setItemMeta(meta);
        return button;
    }
    
    /**
     * 替换变量
     * @param text 原始文本
     * @param menuConfig 菜单配置
     * @return 替换后的文本
     */
    private static String replaceVariables(String text, ConfigurationSection menuConfig) {
        return replaceVariables(text, menuConfig, null);
    }
    
    /**
     * 替换文本中的变量
     * @param text 原始文本
     * @param menuConfig 菜单配置
     * @param player 玩家（用于显示正确的成功率，可为null）
     * @return 替换后的文本
     */
    private static String replaceVariables(String text, ConfigurationSection menuConfig, Player player) {
        if (text == null) {
            return text;
        }
        
        // 替换 %success-rate% - 使用玩家缓存的菜单成功率或当前成功率
        if (text.contains("%success-rate%")) {
            int successRate;
            if (player != null && MoRuneSocketing.getInstance().getDataManager() != null) {
                // 使用玩家打开菜单时缓存的成功率
                successRate = MoRuneSocketing.getInstance().getDataManager().getPlayerMenuRate(player.getUniqueId());
            } else {
                // 使用当前成功率
                successRate = MoRuneSocketing.getInstance().getCurrentSuccessRate();
            }
            text = text.replace("%success-rate%", String.valueOf(successRate));
        }
        
        // 替换 %cost% - 镶嵌费用
        if (text.contains("%cost%")) {
            double cost = 0;
            if (MoRuneSocketing.getInstance().getEconomyManager() != null && 
                MoRuneSocketing.getInstance().getEconomyManager().isEnabled()) {
                cost = MoRuneSocketing.getInstance().getEconomyManager().getInsertCost();
            }
            text = text.replace("%cost%", String.valueOf((int) cost));
        }
        
        // 替换 %return-chance% - 符文拆卸返还概率
        if (text.contains("%return-chance%")) {
            int returnChance = MoRuneSocketing.getInstance().getConfig().getInt("rune-remover.return-chance", 100);
            text = text.replace("%return-chance%", String.valueOf(returnChance));
        }
        
        return text;
    }
    
    /**
     * 设置默认菜单布局（兼容旧版本）
     */
    private static void setupDefaultMenu(Inventory menu) {
        // 设置边框
        ItemStack border = createBorderItem();
        int[] borderSlots = {0,1,2,3,4,5,6,7,8,9,11,12,14,15,17,18,19,20,21,22,23,24,25,26};
        
        for (int slot : borderSlots) {
            menu.setItem(slot, border);
        }
        
        // 设置镶嵌按钮
        menu.setItem(DEFAULT_INSERT_BUTTON_SLOT, createDefaultInsertButton());
    }
    
    /**
     * 创建边框物品
     */
    private static ItemStack createBorderItem() {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        return border;
    }
    
    /**
     * 创建默认镶嵌按钮
     */
    private static ItemStack createDefaultInsertButton() {
        ItemStack insertButton = new ItemStack(Material.ANVIL);
        ItemMeta buttonMeta = insertButton.getItemMeta();
        buttonMeta.setDisplayName("§a开始镶嵌");
        buttonMeta.setLore(Arrays.asList("§7点击开始镶嵌符文"));
        
        // 添加自定义标识
        buttonMeta.getPersistentDataContainer().set(
            new NamespacedKey(MoRuneSocketing.getInstance(), NBT_MENU_BUTTON),
            PersistentDataType.STRING,
            "insert_button"
        );
        
        insertButton.setItemMeta(buttonMeta);
        return insertButton;
    }
    
    // ==================== 事件处理 ====================
    
    /**
     * 处理镶嵌菜单点击事件
     * @param event 库存点击事件
     */
    public static void handleInsertMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory menu = event.getInventory();
        int slot = event.getRawSlot();
        
        // 检查是否点击了菜单内的槽位
        if (slot < 0 || slot >= menu.getSize()) {
            return;
        }
        
        ConfigurationSection menuConfig = getMenuConfig();
        if (menuConfig == null) {
            event.setCancelled(true);
            return;
        }
        
        // 获取关键槽位位置（支持自定义配置）
        int equipmentSlot = findEquipmentSlot(menuConfig);
        int runeSlot = findRuneSlot(menuConfig);
        int insertButtonSlot = findInsertButtonSlot(menuConfig);
        
        // 获取点击的物品和按钮类型
        ItemStack clickedItem = event.getCurrentItem();
        String buttonType = getButtonType(clickedItem);
        
        // 处理镶嵌按钮点击
        if (isInsertButtonClicked(buttonType, slot, insertButtonSlot)) {
            handleInsertButtonAction(event, player, menu, equipmentSlot, runeSlot);
            return;
        }
        
        // 处理装备槽和符文槽交互
        if (slot == equipmentSlot || slot == runeSlot) {
            handleSlotInteraction(event, slot, equipmentSlot, runeSlot);
            return;
        }
        
        // 其他槽位禁止交互
        event.setCancelled(true);
    }
    
    /**
     * 获取按钮类型
     */
    private static String getButtonType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(
            new NamespacedKey(MoRuneSocketing.getInstance(), NBT_MENU_BUTTON),
            PersistentDataType.STRING
        );
    }
    
    /**
     * 检查是否点击了镶嵌按钮
     */
    private static boolean isInsertButtonClicked(String buttonType, int slot, int insertButtonSlot) {
        return "I".equals(buttonType) || slot == insertButtonSlot;
    }
    
    /**
     * 处理镶嵌按钮动作
     */
    private static void handleInsertButtonAction(InventoryClickEvent event, Player player, 
                                                  Inventory menu, int equipmentSlot, int runeSlot) {
        event.setCancelled(true);
        
        // 检查冷却时间
        if (isOnCooldown(player)) {
            return;
        }
        
        ItemStack equipment = equipmentSlot >= 0 ? menu.getItem(equipmentSlot) : null;
        ItemStack rune = runeSlot >= 0 ? menu.getItem(runeSlot) : null;
        
        if (!isValidItem(equipment)) {
            sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("menu.invalid-item"));
            return;
        }
        
        if (!isEquipment(equipment)) {
            sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("menu.invalid-equipment"));
            return;
        }
        
        if (!isValidItem(rune)) {
            sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("menu.invalid-rune"));
            return;
        }
        
        // 检查是否是符文拆卸粉
        if (isRuneRemover(rune)) {
            handleRuneRemoverAction(player, menu, equipmentSlot, runeSlot, equipment);
            return;
        }
        
        if (!isRune(rune)) {
            sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("menu.wrong-rune"));
            return;
        }
        
        // 设置冷却时间
        setCooldown(player);
        
        // 提前检查符文是否可以镶嵌（避免金币扣除后才发现无法镶嵌）
        boolean canApplyRune = canApplyRuneToItem(equipment, rune);
        if (!canApplyRune) {
            // 检查具体失败原因并给出相应提示
            String failReason = getRuneApplyFailReason(equipment, rune);
            sendMessage(player, failReason);
            playSound(player, false);
            return;
        }
        
        // 检查经济系统
        boolean deductOnSuccessOnly = MoRuneSocketing.getInstance().getConfig()
            .getBoolean("socketing.economy.deduct-on-success-only", false);
        
        if (MoRuneSocketing.getInstance().getEconomyManager() != null && 
            MoRuneSocketing.getInstance().getEconomyManager().isEnabled()) {
            double cost = MoRuneSocketing.getInstance().getEconomyManager().getInsertCost();
            
            // 检查是否只在成功时扣除金币
            if (!deductOnSuccessOnly) {
                if (!MoRuneSocketing.getInstance().getEconomyManager().hasEnough(player, cost)) {
                    String insufficientMessage = MoRuneSocketing.getInstance().getLanguageManager()
                        .getMessage("insufficient-funds")
                        .replace("%cost%", MoRuneSocketing.getInstance().getEconomyManager().format(cost));
                    sendMessage(player, insufficientMessage);
                    playSound(player, false);
                    return;
                }
                MoRuneSocketing.getInstance().getEconomyManager().withdraw(player, cost);
            } else {
                if (!MoRuneSocketing.getInstance().getEconomyManager().hasEnough(player, cost)) {
                    String insufficientMessage = MoRuneSocketing.getInstance().getLanguageManager()
                        .getMessage("insufficient-funds")
                        .replace("%cost%", MoRuneSocketing.getInstance().getEconomyManager().format(cost));
                    sendMessage(player, insufficientMessage);
                    playSound(player, false);
                    return;
                }
            }
        }
        
        // 检查镶嵌成功率
        int successRate = MoRuneSocketing.getInstance().getCurrentSuccessRate();
        boolean success = Math.random() * 100 < successRate;
        
        if (success && applyRuneToItem(equipment, rune)) {
            // 镶嵌成功
            if (deductOnSuccessOnly && MoRuneSocketing.getInstance().getEconomyManager() != null && 
                MoRuneSocketing.getInstance().getEconomyManager().isEnabled()) {
                // 只在成功时扣除金币
                MoRuneSocketing.getInstance().getEconomyManager().withdraw(player, 
                    MoRuneSocketing.getInstance().getEconomyManager().getInsertCost());
            }
            
            sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("insert-success"));
            playSound(player, true);
            consumeRune(menu, rune, runeSlot);
            menu.setItem(equipmentSlot, equipment);
        } else if (!success) {
            String failedMessage = MoRuneSocketing.getInstance().getLanguageManager()
                .getMessage("insert-failed");
            sendMessage(player, failedMessage + " (成功率: " + successRate + "%)");
            playSound(player, false);
            if (MoRuneSocketing.getInstance().getConfig().getBoolean("features.consume-rune-on-fail", true)) {
                consumeRune(menu, rune, runeSlot);
            }
        } else {
            String failedMessage = MoRuneSocketing.getInstance().getLanguageManager()
                .getMessage("insert-failed");
            sendMessage(player, failedMessage);
            playSound(player, false);
        }
    }
    
    /**
     * 检查玩家是否在冷却中
     */
    private static boolean isOnCooldown(Player player) {
        String playerId = player.getUniqueId().toString();
        Long lastTime = socketingCooldowns.get(playerId);
        
        if (lastTime == null) {
            return false;
        }
        
        double cooldownSeconds = getSocketingCooldown();
        long cooldownMs = (long) (cooldownSeconds * 1000);
        
        return System.currentTimeMillis() - lastTime < cooldownMs;
    }
    
    /**
     * 设置玩家冷却时间
     */
    private static void setCooldown(Player player) {
        socketingCooldowns.put(player.getUniqueId().toString(), System.currentTimeMillis());
    }
    
    /**
     * 获取镶嵌冷却时间（秒）
     */
    private static double getSocketingCooldown() {
        org.bukkit.configuration.file.FileConfiguration config = MoRuneSocketing.getInstance().getConfig();
        return config.getDouble("socketing.cooldown", 0.5);
    }
    
    /**
     * 播放音效
     */
    private static void playSound(Player player, boolean success) {
        if (!MoRuneSocketing.getInstance().getConfig().getBoolean("features.play-sound", true)) {
            return;
        }
        
        String soundName = success ? 
            MoRuneSocketing.getInstance().getConfig().getString("features.success-sound", "BLOCK_ANVIL_USE") : 
            MoRuneSocketing.getInstance().getConfig().getString("features.fail-sound", "BLOCK_ANVIL_DESTROY");
        
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            try {
                player.playSound(player.getLocation(), soundName, 1.0f, 1.0f);
            } catch (Exception ignored) {
            }
        }
    }
    
    /**
     * 检查物品是否有效
     */
    private static boolean isValidItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }
    
    /**
     * 消耗符文
     */
    private static void consumeRune(Inventory menu, ItemStack rune, int runeSlot) {
        if (!MoRuneSocketing.getInstance().getConfig().getBoolean("features.consume-rune", true)) {
            return;
        }
        
        if (rune.getAmount() > 1) {
            rune.setAmount(rune.getAmount() - 1);
        } else {
            menu.setItem(runeSlot, null);
        }
    }
    
    /**
     * 处理槽位交互
     */
    private static void handleSlotInteraction(InventoryClickEvent event, int slot, 
                                               int equipmentSlot, int runeSlot) {
        Player player = (Player) event.getWhoClicked();
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        
        // 取出操作
        if (isTakeOutOperation(current, cursor)) {
            event.setCancelled(false);
            return;
        }
        
        // 放入操作
        if (isPutInOperation(cursor)) {
            handlePutInOperation(event, player, slot, equipmentSlot, runeSlot, cursor);
        }
    }
    
    /**
     * 检查是否是取出操作
     */
    private static boolean isTakeOutOperation(ItemStack current, ItemStack cursor) {
        return current != null && current.getType() != Material.AIR && 
               (cursor == null || cursor.getType() == Material.AIR);
    }
    
    /**
     * 检查是否是放入操作
     */
    private static boolean isPutInOperation(ItemStack cursor) {
        return cursor != null && cursor.getType() != Material.AIR;
    }
    
    /**
     * 处理放入操作
     */
    private static void handlePutInOperation(InventoryClickEvent event, Player player, 
                                              int slot, int equipmentSlot, int runeSlot, 
                                              ItemStack cursor) {
        if (slot == equipmentSlot) {
            handleEquipmentPutIn(event, player, cursor);
        } else if (slot == runeSlot) {
            handleRunePutIn(event, player, cursor);
        }
    }
    
    /**
     * 处理装备放入
     */
    private static void handleEquipmentPutIn(InventoryClickEvent event, Player player, ItemStack cursor) {
        if (isEquipment(cursor)) {
            event.setCancelled(false);
        } else {
            sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("menu.invalid-equipment"));
            event.setCancelled(true);
        }
    }
    
    private static void handleRunePutIn(InventoryClickEvent event, Player player, ItemStack cursor) {
        if (isRune(cursor) || isRuneRemover(cursor)) {
            event.setCancelled(false);
        } else {
            sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("menu.invalid-rune"));
            event.setCancelled(true);
        }
    }
    
    /**
     * 处理菜单拖放事件
     * @param event 库存拖放事件
     */
    public static void handleMenuDrag(InventoryDragEvent event) {
        ConfigurationSection menuConfig = getMenuConfig();
        if (menuConfig == null) {
            event.setCancelled(true);
            return;
        }
        
        // 从配置中获取装备槽和符文槽位置（支持自定义配置）
        int equipmentSlot = findEquipmentSlot(menuConfig);
        int runeSlot = findRuneSlot(menuConfig);
        
        // 获取拖放的物品
        ItemStack cursor = event.getOldCursor();
        
        for (int slot : event.getRawSlots()) {
            if (slot == equipmentSlot) {
                if (isEquipment(cursor)) {
                    event.setCancelled(false);
                } else {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player player) {
                        sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("menu.cannot-socket-rune"));
                    }
                }
                return;
            } else if (slot == runeSlot) {
                if (isRune(cursor) || isRuneRemover(cursor)) {
                    event.setCancelled(false);
                } else {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player player) {
                        sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("menu.invalid-rune"));
                    }
                }
                return;
            }
        }
        event.setCancelled(true);
    }
    
    /**
     * 处理菜单关闭事件
     * @param event 库存关闭事件
     */
    public static void handleMenuClose(InventoryCloseEvent event) {
        Inventory menu = event.getInventory();
        Player player = (Player) event.getPlayer();
        
        // 清除玩家的菜单成功率缓存
        if (MoRuneSocketing.getInstance().getDataManager() != null) {
            MoRuneSocketing.getInstance().getDataManager().clearPlayerMenuRate(player.getUniqueId());
            // 移除玩家打开菜单记录
            MoRuneSocketing.getInstance().getDataManager().removeOpenMenuPlayer(player.getUniqueId());
        }
        
        ConfigurationSection menuConfig = getMenuConfig();
        if (menuConfig == null) {
            return;
        }
        
        // 从配置中获取装备槽和符文槽位置（支持自定义配置）
        int equipmentSlot = findEquipmentSlot(menuConfig);
        int runeSlot = findRuneSlot(menuConfig);
        
        returnItemsToPlayer(player, menu, equipmentSlot, runeSlot);
    }
    
    /**
     * 刷新玩家的菜单（用于实时更新成功率显示）
     * @param player 要刷新菜单的玩家
     */
    public static void refreshMenu(Player player) {
        // 检查玩家是否在线
        if (!player.isOnline()) {
            return;
        }
        
        // 检查玩家是否打开了菜单
        Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (topInventory == null) {
            return;
        }
        
        // 获取菜单配置
        ConfigurationSection menuConfig = getMenuConfig();
        if (menuConfig == null) {
            return;
        }
        
        // 更新缓存的成功率
        int currentRate = MoRuneSocketing.getInstance().getCurrentSuccessRate();
        MoRuneSocketing.getInstance().getDataManager().cachePlayerMenuRate(player.getUniqueId(), currentRate);
        
        // 重新设置菜单内容
        setupInsertMenu(topInventory, menuConfig, player);
    }
    
    /**
     * 将物品退回给玩家
     */
    private static void returnItemsToPlayer(Player player, Inventory menu, int... slots) {
        for (int slot : slots) {
            ItemStack item = menu.getItem(slot);
            if (isValidItem(item)) {
                // 检查背包是否已满
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                
                // 如果背包满了，将剩余物品掉落在地面上
                if (!leftover.isEmpty()) {
                    for (ItemStack remaining : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                    }
                }
                
                // 清空菜单槽位
                menu.setItem(slot, null);
            }
        }
    }
    
    // ==================== 符文应用 ====================
    
    /**
     * 检查符文是否可以应用到装备上
     * @param item 目标装备
     * @param rune 符文物品
     * @return 是否可以应用
     */
    private static boolean canApplyRuneToItem(ItemStack item, ItemStack rune) {
        if (item == null || rune == null || !rune.hasItemMeta()) {
            return false;
        }
        
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return false;
        }
        
        ItemMeta runeMeta = rune.getItemMeta();
        String runeType = getNbtString(runeMeta, NBT_RUNE_TYPE);
        
        // 首先尝试通过符文类型获取配置
        if (runeType != null) {
            ConfigurationSection runeConfig = getRuneConfig(runeType);
            if (runeConfig != null) {
                return canApplyRuneFromConfig(item, itemMeta, runeConfig);
            }
        }
        
        // 回退到通过符文名称检查
        return canApplyRuneByName(item, itemMeta, runeMeta.getDisplayName());
    }
    
    /**
     * 获取符文应用失败的具体原因
     * @param item 目标装备
     * @param rune 符文物品
     * @return 失败原因描述
     */
    private static String getRuneApplyFailReason(ItemStack item, ItemStack rune) {
        if (item == null || rune == null || !rune.hasItemMeta()) {
            return MoRuneSocketing.getInstance().getLanguageManager().getMessage("invalid-rune-or-item", "§c符文或装备无效！");
        }
        
        ItemMeta itemMeta = item.getItemMeta();
        ItemMeta runeMeta = rune.getItemMeta();
        String runeType = getNbtString(runeMeta, NBT_RUNE_TYPE);
        
        if (runeType != null) {
            ConfigurationSection runeConfig = getRuneConfig(runeType);
            if (runeConfig != null) {
                // 检查runes-items配置
                if (runeConfig.contains("runes-items")) {
                    List<String> runesItems = runeConfig.getStringList("runes-items");
                    if (!runesItems.isEmpty()) {
                        // 获取物品材质名称
                        String materialName = item.getType().name().toUpperCase();
                        
                        // 检查物品是否在runes-items列表中
                        boolean isInRunesItems = false;
                        for (String runesItem : runesItems) {
                            String upperRunesItem = runesItem.toUpperCase();
                            if (upperRunesItem.contains("*")) {
                                String pattern = upperRunesItem.replace("*", "");
                                if (materialName.endsWith(pattern)) {
                                    isInRunesItems = true;
                                    break;
                                }
                            } else if (materialName.equals(upperRunesItem)) {
                                isInRunesItems = true;
                                break;
                            }
                        }
                        
                        if (!isInRunesItems) {
                            return "§c该符文只能镶嵌在特定物品上！";
                        }
                    }
                }
                
                return getRuneApplyFailReasonFromConfig(itemMeta, runeConfig);
            }
        }
        
        return MoRuneSocketing.getInstance().getLanguageManager().getMessage("invalid-rune-config", "§c符文配置无效，无法镶嵌！");
    }
    
    /**
     * 根据符文配置检查是否可以应用
     */
    private static boolean canApplyRuneFromConfig(ItemStack item, ItemMeta itemMeta, ConfigurationSection runeConfig) {
        // 检查runes-items配置
        if (runeConfig.contains("runes-items")) {
            List<String> runesItems = runeConfig.getStringList("runes-items");
            if (!runesItems.isEmpty()) {
                // 获取物品材质名称
                String materialName = item.getType().name().toUpperCase();
                
                // 检查物品是否在runes-items列表中
                boolean isInRunesItems = false;
                for (String runesItem : runesItems) {
                    String upperRunesItem = runesItem.toUpperCase();
                    if (upperRunesItem.contains("*")) {
                        String pattern = upperRunesItem.replace("*", "");
                        if (materialName.endsWith(pattern)) {
                            isInRunesItems = true;
                            break;
                        }
                    } else if (materialName.equals(upperRunesItem)) {
                        isInRunesItems = true;
                        break;
                    }
                }
                
                if (!isInRunesItems) {
                    return false;
                }
            }
        }
        
        String enchantmentName = runeConfig.getString("enchantment");
        String buff = runeConfig.getString("buff");
        int maxLevel = runeConfig.getInt("max-level", DEFAULT_ENCHANT_MAX_LEVEL);
        
        // 优先处理BUFF
        if (buff != null) {
            return canApplyBuffFromConfig(itemMeta, runeConfig, buff);
        }
        
        // 处理附魔
        if (enchantmentName != null) {
            return canApplyEnchantmentByName(itemMeta, enchantmentName, runeConfig);
        }
        
        return false;
    }
    
    /**
     * 根据符文配置获取应用失败原因
     */
    private static String getRuneApplyFailReasonFromConfig(ItemMeta itemMeta, ConfigurationSection runeConfig) {
        String enchantmentName = runeConfig.getString("enchantment");
        String buff = runeConfig.getString("buff");
        
        if (buff != null) {
            return getBuffApplyFailReason(itemMeta, runeConfig, buff);
        }
        
        if (enchantmentName != null) {
            return getEnchantmentApplyFailReason(itemMeta, enchantmentName, runeConfig);
        }
        
        return "§c符文配置无效，无法镶嵌！";
    }
    
    /**
     * 检查是否可以应用BUFF
     */
    private static boolean canApplyBuffFromConfig(ItemMeta itemMeta, ConfigurationSection runeConfig, String buffId) {
        // 检查buff数量限制
        if (!checkBuffLimit(itemMeta, buffId)) {
            return false;
        }
        
        String runeName = runeConfig.getString("name", "§a符文");
        int newBuffLevel = runeConfig.getInt("buff-level", 1);
        
        // 使用buff字段作为唯一标识符进行检测
        String buffIdentifier = runeConfig.getString("buff");
        if (buffIdentifier == null || buffIdentifier.isEmpty()) {
            // 如果没有buff字段，回退到使用显示名称
            buffIdentifier = extractBuffDisplayName(runeName);
        }
        
        // 优先从NBT数据中获取当前等级
        int currentLevel = getBuffLevelFromNBT(itemMeta, buffIdentifier);
        
        // 如果NBT中没有找到，尝试从lore中查找
        if (currentLevel == 0) {
            List<String> lore = getOrCreateLore(itemMeta);
            currentLevel = findAndRemoveBuffByBuffId(lore, buffIdentifier, runeName, itemMeta);
        }
        
        // 检查是否已经是相同等级的BUFF
        if (newBuffLevel == currentLevel) {
            return false;
        }
        
        // 检查新等级是否低于当前等级
        if (newBuffLevel < currentLevel) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取BUFF应用失败原因
     */
    private static String getBuffApplyFailReason(ItemMeta itemMeta, ConfigurationSection runeConfig, String buffId) {
        // 检查buff数量限制
        if (!checkBuffLimit(itemMeta, buffId)) {
            return MoRuneSocketing.getInstance().getLanguageManager().getMessage("max-buff-runes", "§c该装备已达到最大buff符文数量限制！");
        }
        
        String runeName = runeConfig.getString("name", "§a符文");
        int newBuffLevel = runeConfig.getInt("buff-level", 1);
        
        // 使用buff字段作为唯一标识符进行检测
        String buffIdentifier = runeConfig.getString("buff");
        if (buffIdentifier == null || buffIdentifier.isEmpty()) {
            // 如果没有buff字段，回退到使用显示名称
            buffIdentifier = extractBuffDisplayName(runeName);
        }
        
        // 优先从NBT数据中获取当前等级
        int currentLevel = getBuffLevelFromNBT(itemMeta, buffIdentifier);
        
        // 如果NBT中没有找到，尝试从lore中查找
        if (currentLevel == 0) {
            List<String> lore = getOrCreateLore(itemMeta);
            currentLevel = findAndRemoveBuffByBuffId(lore, buffIdentifier, runeName, itemMeta);
        }
        
        // 检查是否已经是相同等级的BUFF
        if (newBuffLevel == currentLevel) {
            return MoRuneSocketing.getInstance().getLanguageManager().getMessage("same-buff-level", "§c该装备已拥有相同等级的BUFF符文，无法镶嵌！");
        }
        
        // 检查新等级是否低于当前等级
        if (newBuffLevel < currentLevel) {
            return MoRuneSocketing.getInstance().getLanguageManager().getMessage("lower-buff-level", "§c该装备已拥有更高等级的BUFF符文（当前：%current_level%级，符文：%rune_level%级），无法镶嵌！")
                    .replace("%current_level%", String.valueOf(currentLevel))
                    .replace("%rune_level%", String.valueOf(newBuffLevel));
        }
        
        return MoRuneSocketing.getInstance().getLanguageManager().getMessage("buff-apply-unknown-error", "§c无法应用BUFF，未知原因！");
    }
    
    /**
     * 检查是否可以应用附魔
     */
    private static boolean canApplyEnchantmentByName(ItemMeta itemMeta, String enchantmentName, ConfigurationSection runeConfig) {
        int maxLevel = runeConfig.getInt("max-level", DEFAULT_ENCHANT_MAX_LEVEL);
        
        Enchantment enchantment = getEnchantmentByName(enchantmentName);
        if (enchantment == null) {
            return false;
        }
        
        // 检查是否需要装备拥有对应附魔
        boolean requireEnchantment = MoRuneSocketing.getInstance().getConfig()
            .getBoolean("features.require-enchantment-for-rune", true);
        
        if (requireEnchantment && !itemMeta.hasEnchant(enchantment)) {
            return false;
        }
        
        int currentLevel = itemMeta.getEnchantLevel(enchantment);
        int newLevel = currentLevel + 1;
        
        return newLevel <= maxLevel;
    }
    
    /**
     * 获取附魔应用失败原因
     */
    private static String getEnchantmentApplyFailReason(ItemMeta itemMeta, String enchantmentName, ConfigurationSection runeConfig) {
        int maxLevel = runeConfig.getInt("max-level", DEFAULT_ENCHANT_MAX_LEVEL);
        
        Enchantment enchantment = getEnchantmentByName(enchantmentName);
        if (enchantment == null) {
            return MoRuneSocketing.getInstance().getLanguageManager().getMessage("invalid-enchantment-type", "§c无效的附魔类型：%enchantment%！").replace("%enchantment%", enchantmentName);
        }
        
        // 检查是否需要装备拥有对应附魔
        boolean requireEnchantment = MoRuneSocketing.getInstance().getConfig()
            .getBoolean("features.require-enchantment-for-rune", true);
        
        if (requireEnchantment && !itemMeta.hasEnchant(enchantment)) {
            return MoRuneSocketing.getInstance().getLanguageManager().getMessage("missing-enchantment", "§c该装备需要先拥有相同附魔才能使用此符文！");
        }
        
        int currentLevel = itemMeta.getEnchantLevel(enchantment);
        int newLevel = currentLevel + 1;
        
        if (newLevel > maxLevel) {
            return MoRuneSocketing.getInstance().getLanguageManager().getMessage("max-enchantment-level", "§c该装备的相同附魔已达到该符文的最大等级限制！");
        }
        
        return MoRuneSocketing.getInstance().getLanguageManager().getMessage("enchantment-apply-unknown-error", "§c无法应用附魔，未知原因！");
    }
    
    /**
     * 根据符文名称检查是否可以应用（兼容旧版）
     */
    private static boolean canApplyRuneByName(ItemStack item, ItemMeta itemMeta, String runeName) {
        ConfigurationSection runeConfig = getRuneConfigByName(runeName);
        if (runeConfig != null) {
            return canApplyRuneFromConfig(item, itemMeta, runeConfig);
        }
        
        // 兼容旧版：根据符文名称关键词判断
        return canApplyRuneByKeyword(itemMeta, runeName);
    }
    
    /**
     * 根据关键词检查是否可以应用符文（兼容旧版）
     */
    private static boolean canApplyRuneByKeyword(ItemMeta itemMeta, String runeName) {
        if (runeName.contains(KEYWORD_LUCK)) {
            return canApplyLuckBuff(itemMeta, runeName);
        }
        
        // 其他类型的符文默认可以应用（由applyRuneByKeyword处理具体逻辑）
        return true;
    }
    
    /**
     * 检查是否可以应用幸运BUFF
     */
    private static boolean canApplyLuckBuff(ItemMeta itemMeta, String runeName) {
        int runeLevel = extractRuneLevel(runeName);
        List<String> lore = getOrCreateLore(itemMeta);
        
        // 查找已有的幸运BUFF
        int currentLevel = findAndRemoveLuckBuff(lore);
        
        // 检查等级
        return runeLevel > currentLevel;
    }
    
    /**
     * 将符文应用到装备上
     * @param item 目标装备
     * @param rune 符文物品
     * @return 是否成功应用
     */
    private static boolean applyRuneToItem(ItemStack item, ItemStack rune) {
        if (item == null || rune == null || !rune.hasItemMeta()) {
            return false;
        }
        
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return false;
        }
        
        ItemMeta runeMeta = rune.getItemMeta();
        String runeType = getNbtString(runeMeta, NBT_RUNE_TYPE);
        
        // 首先尝试通过符文类型获取配置
        if (runeType != null) {
            ConfigurationSection runeConfig = getRuneConfig(runeType);
            if (runeConfig != null) {
                return applyRuneFromConfig(item, itemMeta, runeConfig);
            }
        }
        
        // 回退到通过符文名称应用效果
        return applyRuneByName(item, itemMeta, runeMeta.getDisplayName());
    }
    
    /**
     * 根据符文配置应用效果
     */
    private static boolean applyRuneFromConfig(ItemStack item, ItemMeta itemMeta, ConfigurationSection runeConfig) {
        String enchantmentName = runeConfig.getString("enchantment");
        String buff = runeConfig.getString("buff");
        int maxLevel = runeConfig.getInt("max-level", DEFAULT_ENCHANT_MAX_LEVEL);
        
        // 优先处理BUFF
        if (buff != null) {
            return applyBuffFromConfig(item, itemMeta, runeConfig, buff);
        }
        
        // 处理附魔
        if (enchantmentName != null) {
            return applyEnchantmentByName(item, itemMeta, enchantmentName, runeConfig);
        }
        
        return false;
    }
    
    /**
     * 根据配置应用BUFF
     */
    private static boolean applyBuffFromConfig(ItemStack item, ItemMeta itemMeta, 
                                               ConfigurationSection runeConfig, String buffId) {
        String runeName = runeConfig.getString("name", "§a符文");
        int newBuffLevel = runeConfig.getInt("buff-level", 1);
        int maxLevel = runeConfig.getInt("max-level", DEFAULT_BUFF_MAX_LEVEL);
        
        // 使用buff字段作为唯一标识符进行检测
        String buffIdentifier = runeConfig.getString("buff");
        if (buffIdentifier == null || buffIdentifier.isEmpty()) {
            // 如果没有buff字段，回退到使用显示名称
            buffIdentifier = extractBuffDisplayName(runeName);
        }
        
        // 检查buff数量限制
        if (!checkBuffLimit(itemMeta, buffId)) {
            return false;
        }
        
        List<String> lore = getOrCreateLore(itemMeta);
        
        // 使用BUFF标识符查找并移除已有的同BUFF，并获取当前等级
        int currentLevel = findAndRemoveBuffByBuffId(lore, buffIdentifier, runeName, itemMeta);
        
        // 检查是否已经是相同等级的BUFF
        if (newBuffLevel == currentLevel) {
            return false;
        }
        
        // 检查新等级是否低于当前等级
        if (newBuffLevel < currentLevel) {
            return false;
        }
        
        // 确保不超过最大等级
        if (newBuffLevel > maxLevel) {
            newBuffLevel = maxLevel;
        }
        
        // 使用name字段显示，格式为：name: buff-level级
        String displayText = runeName + ": §f" + newBuffLevel + "级";
        
        // 添加新的BUFF描述（使用name字段显示）
        lore.add(displayText);
        
        // 存储多个buff的NBT数据（使用buff字段作为标识符，同时存储显示名称）
        updateBuffsNBT(itemMeta, buffId, newBuffLevel, runeName);
        
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return true;
    }
    
    /**
     * 检查装备上的buff数量是否超过限制
     */
    private static boolean checkBuffLimit(ItemMeta itemMeta, String newBuffId) {
        int maxBuffRunes = MoRuneSocketing.getInstance().getConfig()
            .getInt("features.max-buff-runes-per-item", 3);
        
        String existingBuffs = getNbtString(itemMeta, NBT_CUSTOM_BUFF);
        Map<String, Integer> buffsMap = parseBuffsData(existingBuffs);
        
        // 如果是升级已有buff，不计算数量
        if (buffsMap.containsKey(newBuffId)) {
            return true;
        }
        
        // 检查是否超过限制
        return buffsMap.size() < maxBuffRunes;
    }
    
    /**
     * 更新多个buff的NBT数据
     * 格式: buffId1:level1;buffId2:level2
     */
    private static void updateBuffsNBT(ItemMeta itemMeta, String buffId, int level, String displayName) {
        String existingBuffs = getNbtString(itemMeta, NBT_CUSTOM_BUFF);
        Map<String, Integer> buffsMap = parseBuffsData(existingBuffs);
        
        buffsMap.put(buffId, level);
        
        String newBuffsData = toBuffsData(buffsMap);
        
        itemMeta.getPersistentDataContainer().set(
            new NamespacedKey(MoRuneSocketing.getInstance(), NBT_CUSTOM_BUFF),
            PersistentDataType.STRING,
            newBuffsData
        );
        
        // 同时更新显示名称映射
        updateBuffDisplayMap(itemMeta, buffId, displayName);
    }
    
    /**
     * 更新buff显示名称映射
     * 格式: buffId1:displayName1;buffId2:displayName2
     */
    private static void updateBuffDisplayMap(ItemMeta itemMeta, String buffId, String displayName) {
        String existingMap = getNbtString(itemMeta, NBT_BUFF_DISPLAY_MAP);
        Map<String, String> displayMap = parseBuffDisplayMap(existingMap);
        
        displayMap.put(buffId, displayName);
        
        String newMapData = toBuffDisplayMap(displayMap);
        
        itemMeta.getPersistentDataContainer().set(
            new NamespacedKey(MoRuneSocketing.getInstance(), NBT_BUFF_DISPLAY_MAP),
            PersistentDataType.STRING,
            newMapData
        );
    }
    
    /**
     * 解析buff显示名称映射
     */
    private static Map<String, String> parseBuffDisplayMap(String data) {
        Map<String, String> map = new HashMap<>();
        if (data == null || data.isEmpty()) {
            return map;
        }
        
        String[] entries = data.split(";");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;
            
            int colonIndex = entry.indexOf(':');
            if (colonIndex > 0 && colonIndex < entry.length() - 1) {
                String key = entry.substring(0, colonIndex).trim();
                String value = entry.substring(colonIndex + 1).trim();
                map.put(key, value);
            }
        }
        
        return map;
    }
    
    /**
     * 将buff显示名称映射转换为字符串
     */
    private static String toBuffDisplayMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                sb.append(";");
            }
            first = false;
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }
    
    /**
     * 将buff map转换为简单字符串格式
     */
    private static String toBuffsData(Map<String, Integer> buffsMap) {
        if (buffsMap == null || buffsMap.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : buffsMap.entrySet()) {
            if (!first) {
                sb.append(";");
            }
            first = false;
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }
    
    /**
     * 从字符串解析buff map
     */
    public static Map<String, Integer> parseBuffsData(String data) {
        // 空数据快速返回
        if (data == null || data.isEmpty()) {
            return new HashMap<>();
        }
        
        // 简单的缓存机制，避免重复解析相同数据
        if (data.equals(lastParsedData) && lastParsedResult != null) {
            return new HashMap<>(lastParsedResult); // 返回副本避免修改缓存
        }
        
        Map<String, Integer> buffsMap = new HashMap<>();
        
        // 优化字符串分割：手动分割避免创建多个数组
        int start = 0;
        int end = data.indexOf(';');
        
        while (start < data.length()) {
            String buff;
            if (end == -1) {
                buff = data.substring(start);
                start = data.length();
            } else {
                buff = data.substring(start, end);
                start = end + 1;
                end = data.indexOf(';', start);
            }
            
            if (!buff.isEmpty()) {
                // 手动分割键值对
                int colonIndex = buff.indexOf(':');
                if (colonIndex > 0 && colonIndex < buff.length() - 1) {
                    String key = buff.substring(0, colonIndex).trim();
                    String value = buff.substring(colonIndex + 1).trim();
                    
                    try {
                        buffsMap.put(key, Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        // 忽略解析错误
                    }
                }
            }
        }
        
        // 更新缓存
        lastParsedData = data;
        lastParsedResult = new HashMap<>(buffsMap);
        
        return buffsMap;
    }
    
    /**
     * 提取BUFF显示名称（移除符文名称中的等级信息）
     */
    private static String extractBuffDisplayName(String runeName) {
        if (runeName == null || runeName.isEmpty()) {
            return "";
        }
        
        // 使用更高效的字符串处理方法替换正则表达式
        String result = runeName;
        
        // 查找并移除等级后缀
        int levelIndex = result.lastIndexOf("级");
        if (levelIndex > 0) {
            // 向前查找数字
            int digitStart = levelIndex - 1;
            while (digitStart >= 0 && Character.isDigit(result.charAt(digitStart))) {
                digitStart--;
            }
            
            // 移除数字和"级"字
            if (digitStart >= 0) {
                // 检查是否前面有颜色代码或空格
                if (digitStart > 0 && result.charAt(digitStart) == ' ') {
                    // 移除空格和数字级
                    result = result.substring(0, digitStart).trim();
                } else if (digitStart > 0 && result.charAt(digitStart) == '§') {
                    // 移除颜色代码和数字级
                    result = result.substring(0, digitStart).trim();
                }
            }
        }
        
        return result.trim();
    }
    
    /**
     * 根据名称查找并移除BUFF，返回当前等级
     */
    private static int findAndRemoveBuffByName(List<String> lore, String buffName) {
        int currentLevel = 0;
        String targetPattern = buffName + ": §f";
        
        for (String line : lore) {
            if (line.startsWith(targetPattern)) {
                currentLevel = extractLevelFromLine(line, buffName);
                break;
            }
        }
        
        // 精确移除匹配的BUFF行
        lore.removeIf(line -> line.startsWith(targetPattern));
        return currentLevel;
    }
    
    /**
     * 根据buff标识符查找并移除BUFF，返回当前等级
     * 使用buff字段进行检测，通过NBT中存储的显示名称来查找和移除lore行
     */
    private static int findAndRemoveBuffByBuffId(List<String> lore, String buffIdentifier, String runeName, ItemMeta itemMeta) {
        int currentLevel = 0;
        String storedDisplayName = null;
        
        // 首先尝试从NBT中获取已存储的显示名称
        if (itemMeta != null && buffIdentifier != null && !buffIdentifier.isEmpty()) {
            String existingMap = getNbtString(itemMeta, NBT_BUFF_DISPLAY_MAP);
            Map<String, String> displayMap = parseBuffDisplayMap(existingMap);
            storedDisplayName = displayMap.get(buffIdentifier);
        }
        
        // 如果NBT中有存储的显示名称，使用它来查找和移除
        if (storedDisplayName != null) {
            String storedPattern = storedDisplayName + ": §f";
            
            // 查找当前等级
            for (String line : lore) {
                if (line.startsWith(storedPattern)) {
                    currentLevel = extractLevelFromLine(line, storedDisplayName);
                    break;
                }
            }
            
            // 移除所有与该buff标识符相关的lore行（包括存储的名称和当前名称）
            lore.removeIf(line -> {
                // 移除存储的显示名称对应的行
                if (line.startsWith(storedPattern)) {
                    return true;
                }
                // 同时移除当前符文名称对应的行（防止重复）
                if (line.startsWith(runeName + ": §f")) {
                    return true;
                }
                return false;
            });
        } else {
            // 如果NBT中没有找到，使用当前的runeName
            String targetPattern = runeName + ": §f";
            
            for (String line : lore) {
                if (line.startsWith(targetPattern)) {
                    currentLevel = extractLevelFromLine(line, runeName);
                    break;
                }
            }
            
            // 精确移除匹配的BUFF行
            lore.removeIf(line -> line.startsWith(targetPattern));
        }
        
        return currentLevel;
    }
    
    /**
     * 智能查找并移除BUFF，返回当前等级
     * 优先使用buff字段查找，如果没有buff字段则使用显示名称
     */
    private static int findAndRemoveBuffSmart(List<String> lore, String buffId, String displayName, ItemMeta itemMeta) {
        int currentLevel = 0;
        
        // 如果有buff字段，尝试在NBT数据中查找
        if (buffId != null && !buffId.isEmpty() && itemMeta != null) {
            // 从NBT数据中获取当前等级
            currentLevel = getBuffLevelFromNBT(itemMeta, buffId);
            
            if (currentLevel > 0) {
                // 如果从NBT数据中找到，获取存储的显示名称并移除lore中的对应行
                String existingMap = getNbtString(itemMeta, NBT_BUFF_DISPLAY_MAP);
                Map<String, String> displayMap = parseBuffDisplayMap(existingMap);
                String storedDisplayName = displayMap.get(buffId);
                
                if (storedDisplayName != null) {
                    lore.removeIf(line -> line.startsWith(storedDisplayName + ": §f"));
                } else {
                    // 如果没有存储的显示名称，使用传入的displayName
                    lore.removeIf(line -> line.startsWith(displayName + ": §f"));
                }
                return currentLevel;
            }
        }
        
        // 如果没有buff字段或NBT数据中没找到，回退到使用显示名称查找
        String targetPattern = displayName + ": §f";
        
        for (String line : lore) {
            if (line.startsWith(targetPattern)) {
                currentLevel = extractLevelFromLine(line, displayName);
                break;
            }
        }
        
        // 精确移除匹配的BUFF行
        lore.removeIf(line -> line.startsWith(targetPattern));
        return currentLevel;
    }
    
    /**
     * 从NBT数据中获取BUFF等级
     */
    private static int getBuffLevelFromNBT(ItemMeta itemMeta, String buffId) {
        if (itemMeta == null || buffId == null || buffId.isEmpty()) {
            return 0;
        }
        
        String existingBuffs = getNbtString(itemMeta, NBT_CUSTOM_BUFF);
        Map<String, Integer> buffsMap = parseBuffsData(existingBuffs);
        
        return buffsMap.getOrDefault(buffId, 0);
    }
    
    /**
     * 获取NBT字符串值
     */
    private static String getNbtString(ItemMeta meta, String key) {
        return meta.getPersistentDataContainer().get(
            new NamespacedKey(MoRuneSocketing.getInstance(), key),
            PersistentDataType.STRING
        );
    }
    
    /**
     * 根据符文名称应用效果
     */
    private static boolean applyRuneByName(ItemStack item, ItemMeta itemMeta, String runeName) {
        // 获取符文配置
        ConfigurationSection runeConfig = getRuneConfigByName(runeName);
        if (runeConfig == null) {
            return false;
        }
        
        String enchantmentName = runeConfig.getString("enchantment");
        String buff = runeConfig.getString("buff");
        
        // 优先处理BUFF
        if (buff != null) {
            return applyCustomBuff(item, itemMeta, buff);
        }
        
        // 处理附魔
        if (enchantmentName != null) {
            return applyEnchantmentByName(item, itemMeta, enchantmentName, runeConfig);
        }
        
        // 兼容旧版：根据符文名称关键词判断
        return applyRuneByKeyword(item, itemMeta, runeName);
    }
    
    /**
     * 根据符文名称获取符文配置
     */
    private static ConfigurationSection getRuneConfigByName(String runeName) {
        ConfigurationSection allRunes = getAllRunesConfig();
        if (allRunes == null) {
            return null;
        }
        
        for (String runeId : allRunes.getKeys(false)) {
            ConfigurationSection runeConfig = getRuneConfig(runeId);
            if (runeConfig != null) {
                String configName = runeConfig.getString("name");
                if (configName != null && configName.equals(runeName)) {
                    return runeConfig;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 根据附魔名称应用附魔
     */
    private static boolean applyEnchantmentByName(ItemStack item, ItemMeta itemMeta, 
                                                String enchantmentName, ConfigurationSection runeConfig) {
        // 从配置中读取max-level
        int maxLevel = runeConfig.getInt("max-level", DEFAULT_ENCHANT_MAX_LEVEL);
        
        // 将附魔名称转换为Bukkit附魔
        Enchantment enchantment = getEnchantmentByName(enchantmentName);
        if (enchantment == null) {
            return false;
        }
        
        // 检查是否需要装备拥有对应附魔
        boolean requireEnchantment = MoRuneSocketing.getInstance().getConfig()
            .getBoolean("features.require-enchantment-for-rune", true);
        
        if (requireEnchantment && !itemMeta.hasEnchant(enchantment)) {
            return false;
        }
        
        return applyEnchantment(item, itemMeta, enchantment, 1, maxLevel);
    }
    
    /**
     * 根据名称获取附魔
     */
    private static Enchantment getEnchantmentByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        String lowerName = name.toLowerCase();
        
        // 首先尝试通过Key获取
        try {
            Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(lowerName));
            if (enchantment != null) {
                return enchantment;
            }
        } catch (Exception ignored) {
        }
        
        // 回退到常用附魔名称映射
        return switch (lowerName) {
            case "damage_all", "sharpness" -> Enchantment.DAMAGE_ALL;
            case "damage_undead", "smite" -> Enchantment.DAMAGE_UNDEAD;
            case "damage_arthropods", "bane_of_arthropods" -> Enchantment.DAMAGE_ARTHROPODS;
            case "loot_bonus_mobs", "looting" -> Enchantment.LOOT_BONUS_MOBS;
            case "loot_bonus_blocks", "fortune" -> Enchantment.LOOT_BONUS_BLOCKS;
            case "durability", "unbreaking" -> Enchantment.DURABILITY;
            case "dig_speed", "efficiency" -> Enchantment.DIG_SPEED;
            case "protection_environmental", "protection" -> Enchantment.PROTECTION_ENVIRONMENTAL;
            case "protection_fire", "fire_protection" -> Enchantment.PROTECTION_FIRE;
            case "protection_fall", "feather_falling" -> Enchantment.PROTECTION_FALL;
            case "protection_explosions", "blast_protection" -> Enchantment.PROTECTION_EXPLOSIONS;
            case "protection_projectile", "projectile_protection" -> Enchantment.PROTECTION_PROJECTILE;
            case "oxygen", "respiration" -> Enchantment.OXYGEN;
            case "water_worker", "aqua_affinity" -> Enchantment.WATER_WORKER;
            case "thorns" -> Enchantment.THORNS;
            case "depth_strider" -> Enchantment.DEPTH_STRIDER;
            case "frost_walker" -> Enchantment.FROST_WALKER;
            case "binding_curse" -> Enchantment.BINDING_CURSE;
            case "vanishing_curse" -> Enchantment.VANISHING_CURSE;
            case "sweeping_edge" -> Enchantment.SWEEPING_EDGE;
            case "loyalty" -> Enchantment.LOYALTY;
            case "impaling" -> Enchantment.IMPALING;
            case "riptide" -> Enchantment.RIPTIDE;
            case "channeling" -> Enchantment.CHANNELING;
            case "multishot" -> Enchantment.MULTISHOT;
            case "piercing" -> Enchantment.PIERCING;
            case "quick_charge" -> Enchantment.QUICK_CHARGE;
            case "soul_speed" -> Enchantment.SOUL_SPEED;
            case "swift_sneak" -> Enchantment.SWIFT_SNEAK;
            default -> null;
        };
    }
    
    /**
     * 根据关键词应用符文（兼容旧版）
     */
    private static boolean applyRuneByKeyword(ItemStack item, ItemMeta itemMeta, String runeName) {
        if (runeName.contains(KEYWORD_SHARPNESS)) {
            return applyEnchantment(item, itemMeta, Enchantment.DAMAGE_ALL, 1, DEFAULT_ENCHANT_MAX_LEVEL);
        } else if (runeName.contains(KEYWORD_FORTUNE)) {
            return applyEnchantment(item, itemMeta, Enchantment.LOOT_BONUS_BLOCKS, 1, DEFAULT_ENCHANT_MAX_LEVEL);
        } else if (runeName.contains(KEYWORD_UNBREAKING)) {
            return applyEnchantment(item, itemMeta, Enchantment.DURABILITY, 1, DEFAULT_ENCHANT_MAX_LEVEL);
        } else if (runeName.contains(KEYWORD_EFFICIENCY)) {
            return applyEnchantment(item, itemMeta, Enchantment.DIG_SPEED, 1, DEFAULT_ENCHANT_MAX_LEVEL);
        } else if (runeName.contains(KEYWORD_SMITE)) {
            return applyEnchantment(item, itemMeta, Enchantment.DAMAGE_UNDEAD, 1, DEFAULT_ENCHANT_MAX_LEVEL);
        } else if (runeName.contains(KEYWORD_BANE_OF_ARTHROPODS)) {
            return applyEnchantment(item, itemMeta, Enchantment.DAMAGE_ARTHROPODS, 1, DEFAULT_ENCHANT_MAX_LEVEL);
        } else if (runeName.contains(KEYWORD_LOOTING)) {
            return applyEnchantment(item, itemMeta, Enchantment.LOOT_BONUS_MOBS, 1, DEFAULT_ENCHANT_MAX_LEVEL);
        } else if (runeName.contains(KEYWORD_LUCK)) {
            return applyLuckBuff(item, itemMeta, runeName);
        } else if (runeName.contains(KEYWORD_PROTECTION)) {
            return applyEnchantment(item, itemMeta, Enchantment.PROTECTION_ENVIRONMENTAL, 1, DEFAULT_ENCHANT_MAX_LEVEL);
        }
        
        return false;
    }
    
    /**
     * 应用附魔效果
     */
    private static boolean applyEnchantment(ItemStack item, ItemMeta itemMeta, 
                                             Enchantment enchantment, int addLevel, int maxLevel) {
        int currentLevel = itemMeta.getEnchantLevel(enchantment);
        int newLevel = currentLevel + addLevel;
        
        if (newLevel > maxLevel) {
            return false;
        }
        
        itemMeta.addEnchant(enchantment, newLevel, true);
        item.setItemMeta(itemMeta);
        return true;
    }
    
    /**
     * 获取附魔的显示名称
     */
    private static String getEnchantmentDisplayName(Enchantment enchantment) {
        if (enchantment == null) {
            return "未知附魔";
        }
        
        String key = enchantment.getKey().getKey();
        return switch (key) {
            case "damage_all" -> "锋利";
            case "damage_undead" -> "亡灵杀手";
            case "damage_arthropods" -> "节肢杀手";
            case "loot_bonus_mobs" -> "抢夺";
            case "loot_bonus_blocks" -> "时运";
            case "durability" -> "耐久";
            case "dig_speed" -> "效率";
            case "protection_environmental" -> "保护";
            case "protection_fire" -> "火焰保护";
            case "protection_fall" -> "摔落保护";
            case "protection_explosions" -> "爆炸保护";
            case "protection_projectile" -> "弹射物保护";
            case "oxygen" -> "水下呼吸";
            case "water_worker" -> "水下速掘";
            case "thorns" -> "荆棘";
            case "depth_strider" -> "深海探索者";
            case "frost_walker" -> "冰霜行者";
            case "binding_curse" -> "绑定诅咒";
            case "vanishing_curse" -> "消失诅咒";
            case "sweeping_edge" -> "横扫之刃";
            case "loyalty" -> "忠诚";
            case "impaling" -> "穿刺";
            case "riptide" -> "激流";
            case "channeling" -> "引雷";
            case "multishot" -> "多重射击";
            case "piercing" -> "穿透";
            case "quick_charge" -> "快速装填";
            case "soul_speed" -> "灵魂疾行";
            case "swift_sneak" -> "迅捷潜行";
            default -> key;
        };
    }
    
    /**
     * 应用幸运BUFF效果
     */
    private static boolean applyLuckBuff(ItemStack item, ItemMeta itemMeta, String runeName) {
        int runeLevel = extractRuneLevel(runeName);
        List<String> lore = getOrCreateLore(itemMeta);
        
        // 查找并移除已有的幸运BUFF
        int currentLevel = findAndRemoveLuckBuff(lore);
        
        // 检查等级
        if (currentLevel >= runeLevel) {
            return false;
        }
        
        // 添加新的幸运BUFF描述
        lore.add(String.format(BUFF_DISPLAY_FORMAT, runeLevel));
        
        // 更新NBT数据（确保RuneListener能正确读取）
        updateBuffsNBT(itemMeta, "custom_luck", runeLevel, "幸运BUFF");
        
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return true;
    }
    
    /**
     * 提取符文等级
     */
    private static int extractRuneLevel(String runeName) {
        if (runeName.contains("3") || runeName.contains("luck3")) {
            return 3;
        } else if (runeName.contains("2") || runeName.contains("luck2")) {
            return 2;
        }
        return 1;
    }
    
    /**
     * 获取或创建Lore列表
     */
    private static List<String> getOrCreateLore(ItemMeta itemMeta) {
        List<String> lore = itemMeta.getLore();
        return lore != null ? lore : new ArrayList<>();
    }
    
    /**
     * 查找并移除幸运BUFF，返回当前等级
     */
    private static int findAndRemoveLuckBuff(List<String> lore) {
        int currentLevel = 0;
        
        for (String line : lore) {
            if (line.contains("幸运BUFF")) {
                currentLevel = extractBuffLevelFromLine(line);
                break;
            }
        }
        
        lore.removeIf(line -> line.contains("幸运BUFF"));
        return currentLevel;
    }
    
    /**
     * 从Lore行中提取BUFF等级
     */
    private static int extractBuffLevelFromLine(String line) {
        try {
            String levelStr = line.replace("§a幸运BUFF: §f", "").replace("级", "").trim();
            return Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            // 回退到字符串匹配
            if (line.contains("1级")) return 1;
            if (line.contains("2级")) return 2;
            if (line.contains("3级")) return 3;
            return 0;
        }
    }
    
    /**
     * 应用自定义附魔
     */
    private static boolean applyCustomEnchantment(ItemStack item, ItemMeta itemMeta, String enchantmentId) {
        ConfigurationSection enchantmentConfig = getEnchantmentConfig(enchantmentId);
        
        if (enchantmentConfig == null) {
            return false;
        }
        
        String enchantmentName = enchantmentConfig.getString("name", "§6自定义附魔");
        int maxLevel = enchantmentConfig.getInt("max-level", DEFAULT_ENCHANT_MAX_LEVEL);
        
        List<String> lore = getOrCreateLore(itemMeta);
        int currentLevel = findAndRemoveEnchantment(lore, enchantmentName);
        int newLevel = currentLevel + 1;
        
        if (newLevel > maxLevel) {
            return false;
        }
        
        lore.add(enchantmentName + ": §f" + newLevel + "级");
        
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return true;
    }
    
    /**
     * 获取附魔配置
     */
    private static ConfigurationSection getEnchantmentConfig(String enchantmentId) {
        return MoRuneSocketing.getInstance().getConfig()
            .getConfigurationSection("custom-enchantments." + enchantmentId);
    }
    
    /**
     * 查找并移除附魔，返回当前等级
     */
    private static int findAndRemoveEnchantment(List<String> lore, String enchantmentName) {
        int currentLevel = 0;
        
        for (String line : lore) {
            if (line.contains(enchantmentName)) {
                currentLevel = extractLevelFromLine(line, enchantmentName);
                break;
            }
        }
        
        lore.removeIf(line -> line.contains(enchantmentName));
        return currentLevel;
    }
    
    /**
     * 从行中提取等级
     */
    private static int extractLevelFromLine(String line, String prefix) {
        try {
            // 精确匹配格式："前缀: §f数字级"
            String levelStr = line.replace(prefix + ": §f", "").replace("级", "").trim();
            return Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            // 如果精确解析失败，尝试从字符串中提取数字
            String levelStr = line.replaceAll("[^0-9]", "").trim();
            if (!levelStr.isEmpty()) {
                try {
                    return Integer.parseInt(levelStr);
                } catch (NumberFormatException ex) {
                    return 0;
                }
            }
            return 0;
        }
    }
    
    /**
     * 应用自定义BUFF
     */
    private static boolean applyCustomBuff(ItemStack item, ItemMeta itemMeta, String buffId) {
        // 检查buff数量限制
        if (!checkBuffLimit(itemMeta, buffId)) {
            return false;
        }
        
        // 从符文配置中获取buff-level
        String runeType = getNbtString(itemMeta, NBT_RUNE_TYPE);
        ConfigurationSection runeConfig = runeType != null ? getRuneConfig(runeType) : null;
        
        int buffLevel = 0;
        if (runeConfig != null && runeConfig.contains("buff-level")) {
            buffLevel = runeConfig.getInt("buff-level", 0);
        }
        
        // 如果没有buff-level，尝试从BUFF配置中获取
        ConfigurationSection buffConfig = getBuffConfig(buffId);
        if (buffConfig == null) {
            return false;
        }
        
        String buffName = buffConfig.getString("name", "§a自定义BUFF");
        int maxLevel = buffConfig.getInt("max-level", DEFAULT_BUFF_MAX_LEVEL);
        
        List<String> lore = getOrCreateLore(itemMeta);
        int currentLevel = findAndRemoveBuff(lore, buffName);
        
        // 使用buff-level作为新等级，如果未设置则使用当前等级+1
        int newLevel = buffLevel > 0 ? buffLevel : (currentLevel + 1);
        
        if (newLevel > maxLevel) {
            return false;
        }
        
        lore.add(buffName + ": §f" + newLevel + "级");
        
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        return true;
    }
    
    /**
     * 获取BUFF配置
     */
    private static ConfigurationSection getBuffConfig(String buffId) {
        return MoRuneSocketing.getInstance().getConfig()
            .getConfigurationSection("custom-buffs." + buffId);
    }
    
    /**
     * 查找并移除BUFF，返回当前等级
     */
    private static int findAndRemoveBuff(List<String> lore, String buffName) {
        // 使用统一的BUFF查找和移除方法
        return findAndRemoveBuffByName(lore, buffName);
    }
    
    // ==================== 物品检查 ====================
    
    /**
     * 检查物品是否是装备
     * @param item 要检查的物品
     * @return 是否是装备
     */
    public static boolean isEquipment(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        Material material = item.getType();
        String materialName = material.name();
        
        // 从配置文件中获取可镶嵌物品列表
        List<String> socketableItems = getSocketableItems();
        if (socketableItems == null || socketableItems.isEmpty()) {
            // 如果配置为空，使用默认判断
            return isDefaultEquipment(material, materialName);
        }
        
        // 检查是否在可镶嵌物品列表中
        return isSocketableItem(materialName, socketableItems);
    }
    
    /**
     * 获取可镶嵌物品列表
     */
    private static List<String> getSocketableItems() {
        ConfigurationSection config = MoRuneSocketing.getInstance().getConfig();
        return config.getStringList("socketable-items");
    }
    
    /**
     * 检查物品是否可镶嵌（支持通配符）
     */
    private static boolean isSocketableItem(String materialName, List<String> socketableItems) {
        String upperMaterialName = materialName.toUpperCase();
        
        for (String socketable : socketableItems) {
            String upperSocketable = socketable.toUpperCase();
            
            // 支持通配符*
            if (upperSocketable.contains("*")) {
                String pattern = upperSocketable.replace("*", "");
                if (upperMaterialName.endsWith(pattern)) {
                    return true;
                }
            } else if (upperMaterialName.equals(upperSocketable)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 默认装备判断（兼容旧版）
     */
    private static boolean isDefaultEquipment(Material material, String materialName) {
        return materialName.endsWith("_SWORD") || 
               materialName.endsWith("_AXE") || 
               materialName.endsWith("_PICKAXE") || 
               materialName.endsWith("_SHOVEL") || 
               materialName.endsWith("_HOE") || 
               materialName.endsWith("_HELMET") || 
               materialName.endsWith("_CHESTPLATE") || 
               materialName.endsWith("_LEGGINGS") || 
               materialName.endsWith("_BOOTS") || 
               material == Material.BOW || 
               material == Material.CROSSBOW || 
               material == Material.TRIDENT || 
               material == Material.SHIELD;
    }
    
    /**
     * 检查物品是否是符文
     * @param item 要检查的物品
     * @return 是否是符文
     */
    public static boolean isRune(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = meta.getDisplayName();
        ConfigurationSection runesConfig = getAllRunesConfig();
        
        if (runesConfig == null) {
            return false;
        }
        
        return checkIfRuneByConfig(meta, displayName, runesConfig);
    }
    
    /**
     * 根据配置检查是否是符文
     */
    private static boolean checkIfRuneByConfig(ItemMeta meta, String displayName, 
                                                ConfigurationSection runesConfig) {
        Set<String> runeIds = runesConfig.getKeys(false);
        
        for (String runeId : runeIds) {
            ConfigurationSection runeConfig = getRuneConfig(runeId);
            if (runeConfig == null) {
                continue;
            }
            
            String runeName = runeConfig.getString("name");
            if (runeName == null || !displayName.equals(runeName)) {
                continue;
            }
            
            if (isLoreMatching(meta, runeConfig)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查Lore是否匹配
     */
    private static boolean isLoreMatching(ItemMeta meta, ConfigurationSection runeConfig) {
        List<String> configLore = runeConfig.getStringList("lore");
        
        // 如果配置中没有lore要求，只要名称匹配就返回true
        if (configLore == null || configLore.isEmpty()) {
            return true;
        }
        
        // 如果配置中有lore但物品没有，返回false
        if (!meta.hasLore()) {
            return false;
        }
        
        List<String> itemLore = meta.getLore();
        
        // 检查配置中的所有lore行是否都在物品lore中
        for (String configLine : configLore) {
            boolean lineFound = false;
            for (String itemLine : itemLore) {
                if (itemLine.equals(configLine)) {
                    lineFound = true;
                    break;
                }
            }
            if (!lineFound) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查物品是否包含BUFF效果
     * @param item 要检查的物品
     * @param buffType BUFF类型
     * @return 是否包含该BUFF
     */
    public static boolean hasBuffEffect(ItemStack item, String buffType) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return false;
        }
        
        List<String> lore = meta.getLore();
        
        for (String line : lore) {
            if (line.contains("幸运BUFF:") && buffType.equals("LUCK")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取物品的BUFF等级
     * @param item 物品
     * @param buffType BUFF类型
     * @return BUFF等级，未找到返回0
     */
    public static int getBuffLevel(ItemStack item, String buffType) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return 0;
        }
        
        List<String> lore = meta.getLore();
        
        for (String line : lore) {
            if (line.contains("幸运BUFF:") && buffType.equals("LUCK")) {
                return extractBuffLevelFromLine(line);
            }
        }
        
        return 0;
    }
    
    // ==================== 符文给予 ====================
    
    /**
     * 给予玩家符文
     * @param sender 执行命令的发送者
     * @param args 命令参数
     */
    public static void giveRune(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, COLOR_ERROR + "用法: /mrs give <玩家> <符文ID> [数量]");
            return;
        }
        
        String targetName = args[1];
        String runeId = args[2];
        int amount = parseAmount(args, sender);
        
        if (amount < 0) {
            return;
        }
        
        Player target = org.bukkit.Bukkit.getPlayer(targetName);
        if (target == null) {
            sendMessage(sender, COLOR_ERROR + "找不到玩家 " + targetName + "！");
            return;
        }
        
        ConfigurationSection runeConfig = getRuneConfig(runeId);
        if (runeConfig == null) {
            sendAvailableRunesMessage(sender);
            return;
        }
        
        ItemStack rune = createRuneItem(runeConfig, amount);
        if (rune == null) {
            sendMessage(sender, COLOR_ERROR + "创建符文失败！");
            return;
        }
        
        target.getInventory().addItem(rune);
        sendMessage(sender, COLOR_SUCCESS + "已给予 " + target.getName() + " " + amount + " 个 " + runeConfig.getString("name") + "！");
        sendMessage(target, COLOR_SUCCESS + "你获得了 " + amount + " 个 " + runeConfig.getString("name") + "！");
    }
    
    /**
     * 解析数量参数
     */
    private static int parseAmount(String[] args, CommandSender sender) {
        if (args.length < 4) {
            return MIN_ITEM_AMOUNT;
        }
        
        try {
            int amount = Integer.parseInt(args[3]);
            return Math.max(MIN_ITEM_AMOUNT, Math.min(amount, MAX_ITEM_AMOUNT));
        } catch (NumberFormatException e) {
            sendMessage(sender, COLOR_ERROR + "数量必须是数字！");
            return -1;
        }
    }
    
    /**
     * 发送可用符文列表消息
     */
    private static void sendAvailableRunesMessage(CommandSender sender) {
        sendMessage(sender, COLOR_ERROR + "找不到该符文！");
        sendMessage(sender, COLOR_INFO + "可用的符文ID:");
        
        ConfigurationSection allRunes = getAllRunesConfig();
        if (allRunes != null) {
            for (String runeId : allRunes.getKeys(false)) {
                ConfigurationSection runeConfig = getRuneConfig(runeId);
                if (runeConfig != null) {
                    String runeName = runeConfig.getString("name", runeId);
                    sendMessage(sender, COLOR_INFO + "  - " + runeId + ": " + runeName);
                }
            }
        }
    }
    
    /**
     * 创建符文物品
     * @param runeConfig 符文配置
     * @param amount 数量
     * @return 符文物品
     */
    private static ItemStack createRuneItem(ConfigurationSection runeConfig, int amount) {
        String materialName = runeConfig.getString("material", "PAPER");
        Material material = Material.getMaterial(materialName.toUpperCase());
        
        if (material == null) {
            material = Material.PAPER;
        }
        
        ItemStack rune = new ItemStack(material, amount);
        ItemMeta meta = rune.getItemMeta();
        
        // 设置名称
        String name = runeConfig.getString("name", "§6未知符文");
        meta.setDisplayName(ColorUtils.translate(name));
        
        // 设置Lore
        List<String> lore = buildRuneLore(runeConfig);
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ColorUtils.translate(line));
        }
        meta.setLore(coloredLore);
        
        // 设置NBT数据
        setRuneNbtData(meta, runeConfig);
        
        // 设置物品标志
        setItemFlags(meta);
        
        // 设置自定义模型数据
        if (runeConfig.contains("custom-model-data")) {
            int customModelData = runeConfig.getInt("custom-model-data", 0);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
        }
        
        rune.setItemMeta(meta);
        return rune;
    }
    
    /**
     * 构建符文Lore
     */
    private static List<String> buildRuneLore(ConfigurationSection runeConfig) {
        List<String> lore = runeConfig.getStringList("lore");
        
        if (lore == null || lore.isEmpty()) {
            lore = new ArrayList<>();
            lore.add("§7符文效果");
            
            if (runeConfig.contains("max-level")) {
                int maxLevel = runeConfig.getInt("max-level", DEFAULT_ENCHANT_MAX_LEVEL);
                lore.add("§7最高等级: §f" + maxLevel + "级");
            } else if (runeConfig.contains("buff")) {
                String buffLevel = runeConfig.getName().replace("luck", "");
                if (!buffLevel.isEmpty()) {
                    lore.add("§7BUFF等级: §f" + buffLevel + "级");
                }
            }
        }
        
        return lore;
    }
    
    /**
     * 检查物品是否是符文拆卸粉
     */
    private static boolean isRuneRemover(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        String removerItem = MoRuneSocketing.getInstance().getConfig().getString("rune-remover.item", "SUGAR");
        if (!item.getType().name().equals(removerItem)) {
            return false;
        }
        
        // 检查物品名称和描述
        if (!item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        String expectedName = MoRuneSocketing.getInstance().getConfig().getString("rune-remover.name", "§b符文拆卸粉");
        
        if (!meta.getDisplayName().equals(expectedName)) {
            return false;
        }
        
        // 检查描述
        List<String> expectedLore = MoRuneSocketing.getInstance().getConfig().getStringList("rune-remover.lore");
        if (!expectedLore.isEmpty()) {
            List<String> actualLore = meta.getLore();
            if (actualLore == null || actualLore.size() < expectedLore.size()) {
                return false;
            }
            
            for (int i = 0; i < expectedLore.size(); i++) {
                if (!actualLore.get(i).equals(expectedLore.get(i))) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 处理符文拆卸粉动作
     */
    private static void handleRuneRemoverAction(Player player, Inventory menu, int equipmentSlot, int runeSlot, ItemStack equipment) {
        // 设置冷却时间
        setCooldown(player);
        
        // 拆卸装备上的buff符文
        boolean removed = removeBuffsFromItem(equipment, player);
        
        if (removed) {
            sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("rune-remover.success"));
            playSound(player, true);
            // 消耗拆卸粉
            consumeRune(menu, menu.getItem(runeSlot), runeSlot);
            menu.setItem(equipmentSlot, equipment);
        } else {
            sendMessage(player, MoRuneSocketing.getInstance().getLanguageManager().getMessage("rune-remover.no-buffs"));
            playSound(player, false);
        }
    }
    
    /**
     * 从装备上移除所有buff符文并返还
     */
    private static boolean removeBuffsFromItem(ItemStack item, Player player) {
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return false;
        }
        
        String existingBuffs = getNbtString(itemMeta, NBT_CUSTOM_BUFF);
        if (existingBuffs == null || existingBuffs.isEmpty()) {
            return false;
        }
        
        Map<String, Integer> buffsMap = parseBuffsData(existingBuffs);
        String existingDisplayMap = getNbtString(itemMeta, NBT_BUFF_DISPLAY_MAP);
        Map<String, String> displayMap = parseBuffDisplayMap(existingDisplayMap);
        
        // 检查返还概率
        int returnChance = MoRuneSocketing.getInstance().getConfig().getInt("rune-remover.return-chance", 100);
        
        // 返还符文并清除buff
        for (Map.Entry<String, Integer> entry : buffsMap.entrySet()) {
            String buffId = entry.getKey();
            int level = entry.getValue();
            
            // 根据概率返还符文
            if (Math.random() * 100 < returnChance) {
                ItemStack rune = createRuneFromBuff(buffId, level);
                if (rune != null) {
                    // 给玩家物品
                    player.getInventory().addItem(rune);
                }
            }
        }
        
        // 清除buff数据
        itemMeta.getPersistentDataContainer().remove(new NamespacedKey(MoRuneSocketing.getInstance(), NBT_CUSTOM_BUFF));
        itemMeta.getPersistentDataContainer().remove(new NamespacedKey(MoRuneSocketing.getInstance(), NBT_BUFF_DISPLAY_MAP));
        
        // 更新物品显示
        List<String> lore = getOrCreateLore(itemMeta);
        List<String> newLore = new ArrayList<>();
        for (String line : lore) {
            // 检查是否包含buff相关的关键词
            boolean isBuffLore = false;
            String lowerLine = line.toLowerCase();
            if (line.contains("§7BUFF:") || 
                line.contains("符文:") || 
                lowerLine.contains("幸运") || 
                lowerLine.contains("速度") || 
                lowerLine.contains("力量") || 
                lowerLine.contains("生命提升") || 
                lowerLine.contains("再生") || 
                lowerLine.contains("抗火")) {
                isBuffLore = true;
            }
            if (!isBuffLore) {
                newLore.add(line);
            }
        }
        itemMeta.setLore(newLore);
        item.setItemMeta(itemMeta);
        
        return true;
    }
    
    /**
     * 根据buff类型和等级创建对应的符文物品
     */
    private static ItemStack createRuneFromBuff(String buffId, int level) {
        // 遍历所有符文配置，找到对应buff的符文
        ConfigurationSection runesConfig = getAllRunesConfig();
        if (runesConfig == null) {
            return null;
        }
        
        for (String runeId : runesConfig.getKeys(false)) {
            ConfigurationSection runeConfig = runesConfig.getConfigurationSection(runeId);
            if (runeConfig == null) {
                continue;
            }
            
            String buff = runeConfig.getString("buff");
            int buffLevel = runeConfig.getInt("buff-level", 1);
            
            if (buff != null && buff.equals(buffId) && buffLevel == level) {
                return createRuneItem(runeConfig, 1);
            }
        }
        
        return null;
    }
    
    /**
     * 设置符文NBT数据
     */
    private static void setRuneNbtData(ItemMeta meta, ConfigurationSection runeConfig) {
        String enchantment = runeConfig.getString("enchantment");
        String buff = runeConfig.getString("buff");
        String runeId = runeConfig.getName();
        
        if (enchantment != null) {
            meta.getPersistentDataContainer().set(
                new NamespacedKey(MoRuneSocketing.getInstance(), NBT_CUSTOM_ENCHANTMENT),
                PersistentDataType.STRING,
                enchantment
            );
        }
        
        if (buff != null) {
            meta.getPersistentDataContainer().set(
                new NamespacedKey(MoRuneSocketing.getInstance(), NBT_CUSTOM_BUFF),
                PersistentDataType.STRING,
                buff
            );
        }
        
        meta.getPersistentDataContainer().set(
            new NamespacedKey(MoRuneSocketing.getInstance(), NBT_RUNE_TYPE),
            PersistentDataType.STRING,
            runeId
        );
        
        // 添加防合成标记
        meta.getPersistentDataContainer().set(
            new NamespacedKey(MoRuneSocketing.getInstance(), NBT_NO_CRAFT),
            PersistentDataType.BOOLEAN,
            true
        );
    }
    
    /**
     * 设置物品标志
     */
    private static void setItemFlags(ItemMeta meta) {
        // 防止物品被合成
        meta.setUnbreakable(true);
        
        // 隐藏各种信息
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
    }
}
