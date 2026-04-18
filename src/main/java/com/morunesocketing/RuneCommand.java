package com.morunesocketing;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 符文命令处理器
 * 处理所有与符文相关的命令
 */
public class RuneCommand implements CommandExecutor, TabCompleter {
    
    // ==================== 常量定义 ====================
    
    // 颜色代码
    private static final String COLOR_SUCCESS = "§a";
    private static final String COLOR_ERROR = "§c";
    private static final String COLOR_INFO = "§7";
    private static final String COLOR_HIGHLIGHT = "§6";
    
    // 权限节点
    private static final String PERMISSION_ADMIN = "morunesocketing.admin";
    
    // 命令名称
    private static final String CMD_HELP = "help";
    private static final String CMD_MENU = "menu";
    private static final String CMD_GIVE = "give";
    private static final String CMD_RELOAD = "reload";
    private static final String CMD_SETINTERVAL = "setinterval";
    private static final String CMD_SETRATE = "setrate";
    private static final String CMD_GIVE_REMOVER = "giveremover";
    
    // 数量选项
    private static final List<String> AMOUNT_OPTIONS = Arrays.asList("1", "5", "10", "16", "32", "64");
    
    // 时间单位选项
    private static final List<String> TIME_UNITS = Arrays.asList("s", "m", "h", "d");
    
    // ==================== 命令执行 ====================
    
    /**
     * 获取语言管理器
     */
    private LanguageManager getLang() {
        return MoRuneSocketing.getInstance().getLanguageManager();
    }
    
    /**
     * 发送消息（带前缀）
     */
    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(getLang().getPrefix() + message);
    }
    
    /**
     * 从语言文件获取消息
     */
    private String getMessage(String path) {
        return getLang().getMessage(path);
    }
    
    /**
     * 从语言文件获取消息并替换变量
     */
    private String getMessage(String path, String... replacements) {
        return getLang().getMessage(path, replacements);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 无参数时显示帮助
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        // 处理命令
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case CMD_HELP -> handleHelpCommand(sender);
            case CMD_MENU -> handleMenuCommand(sender);
            case CMD_GIVE -> handleGiveCommand(sender, args);
            case CMD_GIVE_REMOVER -> handleGiveRemoverCommand(sender, args);
            case CMD_RELOAD -> handleReloadCommand(sender);
            case CMD_SETINTERVAL -> handleSetIntervalCommand(sender, args);
            case CMD_SETRATE -> handleSetRateCommand(sender, args);
            default -> handleUnknownCommand(sender);
        };
    }
    
    // ==================== 命令处理器 ====================
    
    /**
     * 处理帮助命令
     */
    private boolean handleHelpCommand(CommandSender sender) {
        showHelp(sender);
        return true;
    }
    
    /**
     * 处理菜单命令
     */
    private boolean handleMenuCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, getMessage("player-only"));
            return true;
        }
        RuneManager.openInsertMenu(player);
        return true;
    }
    
    /**
     * 处理给予符文命令
     */
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        // 检查权限
        if (!hasAdminPermission(sender)) {
            return true;
        }
        
        // 检查参数
        if (args.length < 3) {
            sendMessage(sender, getMessage("command.give.usage"));
            return true;
        }
        
        RuneManager.giveRune(sender, args);
        return true;
    }
    
    /**
     * 处理给予符文拆卸粉命令
     */
    private boolean handleGiveRemoverCommand(CommandSender sender, String[] args) {
        // 检查权限
        if (!hasAdminPermission(sender)) {
            return true;
        }
        
        // 检查参数
        if (args.length < 2) {
            sendMessage(sender, "§c用法: /mrs giveremover <玩家> [数量]");
            return true;
        }
        
        String playerName = args[1];
        int amount = 1;
        
        if (args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) {
                    sendMessage(sender, "§c数量必须是正整数！");
                    return true;
                }
            } catch (NumberFormatException e) {
                sendMessage(sender, "§c数量必须是数字！");
                return true;
            }
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sendMessage(sender, getMessage("command.give.player-not-found", "%player%", playerName));
            return true;
        }
        
        // 创建符文拆卸粉
        ItemStack remover = createRuneRemoverItem();
        remover.setAmount(amount);
        
        // 给予玩家
        targetPlayer.getInventory().addItem(remover);
        
        // 发送成功消息
        sendMessage(sender, getMessage("rune-remover.give-success", "%player%", playerName, "%amount%", String.valueOf(amount)));
        return true;
    }
    
    /**
     * 创建符文拆卸粉物品
     */
    private ItemStack createRuneRemoverItem() {
        String itemType = MoRuneSocketing.getInstance().getConfig().getString("rune-remover.item", "SUGAR");
        ItemStack item = new ItemStack(Material.valueOf(itemType));
        ItemMeta meta = item.getItemMeta();
        
        // 设置名称
        String name = MoRuneSocketing.getInstance().getConfig().getString("rune-remover.name", "§b符文拆卸粉");
        meta.setDisplayName(name);
        
        // 设置描述
        List<String> lore = MoRuneSocketing.getInstance().getConfig().getStringList("rune-remover.lore");
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        
        // 设置自定义模型数据
        if (MoRuneSocketing.getInstance().getConfig().contains("rune-remover.custom-model-data")) {
            int customModelData = MoRuneSocketing.getInstance().getConfig().getInt("rune-remover.custom-model-data", 0);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 处理重载命令
     */
    private boolean handleReloadCommand(CommandSender sender) {
        // 检查权限
        if (!hasAdminPermission(sender)) {
            return true;
        }
        
        // 重载所有配置和系统
        MoRuneSocketing.getInstance().reloadPlugin();
        
        sendMessage(sender, getMessage("command.reload.success"));
        return true;
    }
    
    /**
     * 处理设置刷新间隔命令
     */
    private boolean handleSetIntervalCommand(CommandSender sender, String[] args) {
        // 检查权限
        if (!hasAdminPermission(sender)) {
            return true;
        }
        
        // 检查是否启用随机成功率
        if (!MoRuneSocketing.getInstance().getConfig().getBoolean("features.random-success-rate.enabled", false)) {
            sendMessage(sender, getMessage("command.setinterval.disabled"));
            return true;
        }
        
        // 检查参数
        if (args.length < 2) {
            sendMessage(sender, getMessage("command.setinterval.usage"));
            sendMessage(sender, COLOR_INFO + "示例: /mrs setinterval 30m (30分钟)");
            sendMessage(sender, COLOR_INFO + "当前刷新间隔: " + COLOR_HIGHLIGHT + 
                MoRuneSocketing.getInstance().getConfig().getString("features.random-success-rate.refresh-interval", "1h"));
            return true;
        }
        
        String timeInput = args[1].toLowerCase();
        String unit = "m"; // 默认分钟
        
        // 检查是否指定了单位
        if (args.length >= 3) {
            unit = args[2].toLowerCase();
            if (!TIME_UNITS.contains(unit)) {
                sendMessage(sender, getMessage("command.setinterval.invalid-unit"));
                return true;
            }
        } else {
            // 尝试从输入中解析单位
            if (timeInput.endsWith("s") || timeInput.endsWith("m") || 
                timeInput.endsWith("h") || timeInput.endsWith("d")) {
                unit = timeInput.substring(timeInput.length() - 1);
                timeInput = timeInput.substring(0, timeInput.length() - 1);
            }
        }
        
        // 解析时间数值
        int timeValue;
        try {
            timeValue = Integer.parseInt(timeInput);
            if (timeValue <= 0) {
                sendMessage(sender, getMessage("command.setinterval.invalid-time"));
                return true;
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, getMessage("command.setinterval.invalid-time"));
            return true;
        }
        
        // 构建时间字符串
        String intervalString = timeValue + unit;
        
        // 验证最小间隔（60秒）
        long ticks = MoRuneSocketing.getInstance().parseIntervalToTicks(intervalString);
        if (ticks < 1200) { // 小于60秒 (60 * 20 = 1200 ticks)
            sendMessage(sender, getMessage("command.setinterval.too-short"));
            return true;
        }
        
        // 保存到配置
        MoRuneSocketing.getInstance().getConfig().set("features.random-success-rate.refresh-interval", intervalString);
        MoRuneSocketing.getInstance().saveConfig();
        
        // 重启定时任务
        MoRuneSocketing.getInstance().restartRandomSuccessRateTask();
        
        // 格式化显示
        String unitDisplay = switch (unit) {
            case "s" -> "秒";
            case "m" -> "分钟";
            case "h" -> "小时";
            case "d" -> "天";
            default -> unit;
        };
        
        sendMessage(sender, getMessage("command.setinterval.success", "%time%", String.valueOf(timeValue), "%unit%", unitDisplay));
        return true;
    }
    
    /**
     * 处理设置成功率命令
     */
    private boolean handleSetRateCommand(CommandSender sender, String[] args) {
        // 检查权限
        if (!hasAdminPermission(sender)) {
            return true;
        }
        
        // 检查是否启用随机成功率
        if (!MoRuneSocketing.getInstance().getConfig().getBoolean("features.random-success-rate.enabled", false)) {
            sendMessage(sender, getMessage("command.setrate.disabled"));
            return true;
        }
        
        // 检查参数
        if (args.length < 2) {
            sendMessage(sender, getMessage("command.setrate.usage"));
            sendMessage(sender, getMessage("command.setrate.current", "%rate%", String.valueOf(MoRuneSocketing.getInstance().getCurrentSuccessRate())));
            return true;
        }
        
        // 解析成功率
        int rate;
        try {
            rate = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sendMessage(sender, getMessage("command.setrate.invalid-number"));
            return true;
        }
        
        // 验证范围
        int minRate = MoRuneSocketing.getInstance().getConfig().getInt("features.random-success-rate.min-rate", 50);
        int maxRate = MoRuneSocketing.getInstance().getConfig().getInt("features.random-success-rate.max-rate", 100);
        
        if (rate < minRate || rate > maxRate) {
            sendMessage(sender, getMessage("command.setrate.out-of-range", "%min%", String.valueOf(minRate), "%max%", String.valueOf(maxRate)));
            return true;
        }
        
        // 设置成功率（不更新刷新时间）
        MoRuneSocketing.getInstance().getDataManager().setCurrentSuccessRate(rate);
        
        sendMessage(sender, getMessage("command.setrate.success", "%rate%", String.valueOf(rate)));
        sendMessage(sender, getMessage("command.setrate.time-unchanged"));
        return true;
    }
    
    /**
     * 处理未知命令
     */
    private boolean handleUnknownCommand(CommandSender sender) {
        sendMessage(sender, getMessage("command.unknown"));
        return true;
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查发送者是否有管理员权限
     */
    private boolean hasAdminPermission(CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            sendMessage(sender, getMessage("command.no-admin-permission"));
            return false;
        }
        return true;
    }
    
    /**
     * 显示帮助信息
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.translate(COLOR_HIGHLIGHT + "===== MoRuneSocketing 帮助 ====="));
        sender.sendMessage(ColorUtils.translate(COLOR_SUCCESS + "/mrs help " + COLOR_INFO + "- 显示帮助信息"));
        sender.sendMessage(ColorUtils.translate(COLOR_SUCCESS + "/mrs menu " + COLOR_INFO + "- 打开符文镶嵌菜单"));
        
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage(ColorUtils.translate(COLOR_SUCCESS + "/mrs give <玩家> <符文ID> [数量] " + COLOR_INFO + "- 给予符文石"));
            sender.sendMessage(ColorUtils.translate(COLOR_SUCCESS + "/mrs setinterval <时间> [单位] " + COLOR_INFO + "- 设置刷新间隔"));
            sender.sendMessage(ColorUtils.translate(COLOR_SUCCESS + "/mrs setrate <成功率> " + COLOR_INFO + "- 设置当前成功率"));
            sender.sendMessage(ColorUtils.translate(COLOR_SUCCESS + "/mrs reload " + COLOR_INFO + "- 重载配置文件"));
        }
    }
    
    // ==================== Tab补全 ====================
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return getFirstLevelCompletions(sender, args[0]);
        } else if (args.length == 2) {
            return getSecondLevelCompletions(sender, args);
        } else if (args.length == 3) {
            return getThirdLevelCompletions(args);
        } else if (args.length == 4) {
            return getFourthLevelCompletions(args);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 获取第一级命令补全
     */
    private List<String> getFirstLevelCompletions(CommandSender sender, String input) {
        List<String> completions = new ArrayList<>();
        
        completions.add(CMD_HELP);
        completions.add(CMD_MENU);
        
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            completions.add(CMD_GIVE);
            completions.add(CMD_GIVE_REMOVER);
            completions.add(CMD_SETINTERVAL);
            completions.add(CMD_SETRATE);
            completions.add(CMD_RELOAD);
        }
        
        return filterCompletions(completions, input);
    }
    
    /**
     * 获取第二级命令补全
     */
    private List<String> getSecondLevelCompletions(CommandSender sender, String[] args) {
        String subCommand = args[0].toLowerCase();
        String input = args[1];
        
        return switch (subCommand) {
            case CMD_GIVE, CMD_GIVE_REMOVER -> getOnlinePlayerNames(sender, input);
            case CMD_SETINTERVAL -> getIntervalCompletions(input);
            case CMD_SETRATE -> getRateCompletions(input);
            default -> Collections.emptyList();
        };
    }
    
    /**
     * 获取成功率补全
     */
    private List<String> getRateCompletions(String input) {
        List<String> completions = new ArrayList<>();
        int minRate = MoRuneSocketing.getInstance().getConfig().getInt("features.random-success-rate.min-rate", 50);
        int maxRate = MoRuneSocketing.getInstance().getConfig().getInt("features.random-success-rate.max-rate", 100);
        int currentRate = MoRuneSocketing.getInstance().getCurrentSuccessRate();
        
        completions.add(String.valueOf(currentRate));
        completions.add(String.valueOf(minRate));
        completions.add(String.valueOf(maxRate));
        
        return filterCompletions(completions, input);
    }
    
    /**
     * 获取刷新间隔补全
     */
    private List<String> getIntervalCompletions(String input) {
        List<String> completions = new ArrayList<>();
        completions.add("30s");
        completions.add("60s");
        completions.add("5m");
        completions.add("10m");
        completions.add("30m");
        completions.add("1h");
        completions.add("2h");
        completions.add("6h");
        completions.add("12h");
        completions.add("1d");
        
        return filterCompletions(completions, input);
    }
    
    /**
     * 获取第三级命令补全
     */
    private List<String> getThirdLevelCompletions(String[] args) {
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals(CMD_GIVE)) {
            // 返回符文ID列表
            Set<String> runeIds = RuneManager.getAllRuneIds();
            return filterCompletions(new ArrayList<>(runeIds), args[2]);
        } else if (subCommand.equals(CMD_GIVE_REMOVER)) {
            // 返回数量选项
            return filterCompletions(AMOUNT_OPTIONS, args[2]);
        } else if (subCommand.equals(CMD_SETINTERVAL)) {
            // 返回时间单位
            return filterCompletions(TIME_UNITS, args[2]);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 获取第四级命令补全
     */
    private List<String> getFourthLevelCompletions(String[] args) {
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals(CMD_GIVE)) {
            return filterCompletions(AMOUNT_OPTIONS, args[3]);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 获取在线玩家名称列表
     */
    private List<String> getOnlinePlayerNames(CommandSender sender, String input) {
        List<String> names = new ArrayList<>();
        
        for (Player player : sender.getServer().getOnlinePlayers()) {
            names.add(player.getName());
        }
        
        return filterCompletions(names, input);
    }
    
    /**
     * 过滤补全列表
     */
    private List<String> filterCompletions(List<String> completions, String input) {
        if (input == null || input.isEmpty()) {
            return completions;
        }
        
        String lowerInput = input.toLowerCase();
        List<String> filtered = new ArrayList<>();
        
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(lowerInput)) {
                filtered.add(completion);
            }
        }
        
        return filtered;
    }
}
