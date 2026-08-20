package com.morunesocketing.integration;

import com.morunesocketing.MoRuneSocketing;
import com.morunesocketing.RuneManager;
import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.adapters.item.ItemComponentBukkitItemStack;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Locale;

public final class MythicMobsIntegration implements Listener {
    private static final String DROP_PREFIX = "morunesocketing";

    private final MoRuneSocketing plugin;

    public MythicMobsIntegration(MoRuneSocketing plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicDropLoad(MythicDropLoadEvent event) {
        String dropName = event.getDropName();
        if (dropName == null || dropName.isBlank()) {
            return;
        }

        String normalizedName = dropName.toLowerCase(Locale.ROOT);
        String runeId = event.getArgument();
        int separator = normalizedName.indexOf(':');
        if ((runeId == null || runeId.isBlank()) && separator >= 0
                && normalizedName.substring(0, separator).equals(DROP_PREFIX)) {
            runeId = dropName.substring(separator + 1);
        }
        runeId = parseRuneId(runeId);

        if (runeId == null || runeId.isBlank() || RuneManager.getRuneConfig(runeId) == null) {
            if (normalizedName.equals(DROP_PREFIX) || normalizedName.startsWith(DROP_PREFIX + ":")) {
                plugin.getLogger().warning("Unknown MythicMobs rune drop: " + dropName);
            }
            return;
        }

        event.register(new RuneDrop(runeId));
        plugin.getLogger().info("Registered MythicMobs rune drop: " + runeId);
    }

    private static String parseRuneId(String argument) {
        if (argument == null || argument.isBlank()) {
            return null;
        }
        String value = argument.trim();
        if (value.startsWith("{") && value.endsWith("}")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        for (String token : value.split("[,\\s]+")) {
            if (token.regionMatches(true, 0, "id=", 0, 3)) {
                return token.substring(3).trim();
            }
        }
        return value;
    }

    private static final class RuneDrop implements IItemDrop {
        private final String runeId;

        private RuneDrop(String runeId) {
            this.runeId = runeId;
        }

        @Override
        public AbstractItemStack getDrop(DropMetadata data, double amount) {
            int itemAmount = Math.max(1, (int) Math.round(amount));
            return new ItemComponentBukkitItemStack(RuneManager.createRuneItem(runeId, itemAmount));
        }
    }
}