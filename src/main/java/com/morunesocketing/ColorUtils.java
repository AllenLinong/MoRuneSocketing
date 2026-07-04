package com.morunesocketing;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * 颜色代码工具类
 * 支持所有Minecraft颜色代码格式
 */
public final class ColorUtils {
    
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = 
        LegacyComponentSerializer.legacySection();
    
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = 
        LegacyComponentSerializer.legacyAmpersand();
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    private ColorUtils() {
    }
    
    /**
     * 转换所有颜色代码格式为Minecraft原生格式
     * 支持：
     * - §符号（原生Minecraft颜色代码）
     * - &符号（常用插件格式）
     * - MiniMessage格式（<red>, <gradient>等）
     * - 十六进制颜色（&#RRGGBB 或 #RRGGBB）
     * 
     * @param text 包含颜色代码的文本
     * @return 转换后的文本
     */
    public static String translate(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        
        // 处理MiniMessage格式
        if (result.contains("<") && result.contains(">")) {
            try {
                Component component = MINI_MESSAGE.deserialize(result);
                result = LEGACY_SERIALIZER.serialize(component);
            } catch (Exception e) {
                // MiniMessage解析失败，继续尝试其他格式
            }
        }
        
        // 处理&符号颜色代码
        if (result.contains("&")) {
            result = translateAmpersand(result);
        }
        
        return result;
    }
    
    /**
     * 转换&符号颜色代码为§符号
     */
    public static String translateAmpersand(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && isValidColorCode(chars[i + 1])) {
                chars[i] = '§';
            }
        }
        
        return new String(chars);
    }
    
    /**
     * 检查是否是有效的颜色代码字符
     */
    private static boolean isValidColorCode(char c) {
        return (c >= '0' && c <= '9') ||
               (c >= 'a' && c <= 'f') ||
               (c >= 'A' && c <= 'F') ||
               c == 'k' || c == 'K' ||
               c == 'l' || c == 'L' ||
               c == 'm' || c == 'M' ||
               c == 'n' || c == 'N' ||
               c == 'o' || c == 'O' ||
               c == 'r' || c == 'R' ||
               c == 'x' || c == 'X';
    }
    
    /**
     * 移除所有颜色代码
     */
    public static String stripColor(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();
        
        for (int i = 0; i < chars.length; i++) {
            if ((chars[i] == '§' || chars[i] == '&') && 
                i + 1 < chars.length && 
                isValidColorCode(chars[i + 1])) {
                i++;
                continue;
            }
            
            // 处理十六进制颜色代码 §x§R§R§G§G§B§B
            if ((chars[i] == '§' || chars[i] == '&') && 
                i + 1 < chars.length && 
                (chars[i + 1] == 'x' || chars[i + 1] == 'X')) {
                i += 13;
                continue;
            }
            
            result.append(chars[i]);
        }
        
        return result.toString();
    }
    
    /**
     * 转换颜色代码并返回Component对象
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        String translated = translate(text);
        return LEGACY_SERIALIZER.deserialize(translated);
    }
    
    /**
     * 从Component对象转换为字符串
     */
    public static String fromComponent(Component component) {
        if (component == null) {
            return "";
        }
        return LEGACY_SERIALIZER.serialize(component);
    }
}
