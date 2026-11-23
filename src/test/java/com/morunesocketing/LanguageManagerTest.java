package com.morunesocketing;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LanguageManagerTest {
    @TempDir Path folder;

    @Test
    void existingChineseFileReceivesNewMessagesWithoutLosingCustomText() throws Exception {
        checkLegacyLanguage("zh_CN", "附魔槽位不足");
    }

    @Test
    void existingEnglishFileReceivesNewMessagesWithoutLosingCustomText() throws Exception {
        checkLegacyLanguage("en_US", "Not enough enchantment slots");
    }

    private void checkLegacyLanguage(String language, String expectedText) throws Exception {
        Path languageFolder = Files.createDirectories(folder.resolve("lang"));
        Files.writeString(languageFolder.resolve(language + ".yml"), "prefix: 'Custom prefix'\n");
        var plugin = mock(MoRuneSocketing.class);
        var config = new YamlConfiguration();
        config.set("language", language);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getResource(anyString())).thenAnswer(invocation ->
                getClass().getClassLoader().getResourceAsStream(invocation.getArgument(0)));
        var manager = new LanguageManager(plugin);
        manager.initialize();

        assertEquals("Custom prefix", manager.getPrefix());
        String message = manager.getMessage("enchantment-slots.limit-reached", "%used%", "4", "%limit%", "3");
        assertTrue(message.contains(expectedText));
        assertTrue(message.contains("4"));
        assertTrue(message.contains("3"));
        assertFalse(manager.getMessage("payment-failed").contains("消息未找到"));
        assertEquals("prefix: 'Custom prefix'\n", Files.readString(languageFolder.resolve(language + ".yml")));
    }
}
