package com.morunesocketing.util;

import com.morunesocketing.MoRuneSocketing;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class SchedulerUtils {

    private static FoliaLib foliaLib;

    private static FoliaLib getFoliaLib() {
        if (foliaLib == null) {
            Plugin plugin = MoRuneSocketing.getInstance();
            foliaLib = new FoliaLib(plugin);
        }
        return foliaLib;
    }

    public static boolean isFolia() {
        return getFoliaLib().isFolia();
    }

    public static void runTask(Runnable task) {
        getFoliaLib().getScheduler().runNextTick((wrappedTask) -> task.run());
    }

    public static void runTaskLater(Runnable task, long delayTicks) {
        getFoliaLib().getScheduler().runLater(task, delayTicks);
    }

    public static WrappedTask runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        return getFoliaLib().getScheduler().runTimer(task, delayTicks, periodTicks);
    }

    public static void runTaskForEntity(Entity entity, Runnable task) {
        getFoliaLib().getScheduler().runAtEntity(entity, (wrappedTask) -> task.run());
    }

    public static void runTaskAtLocation(Location location, Runnable task) {
        getFoliaLib().getScheduler().runAtLocation(location, (wrappedTask) -> task.run());
    }

    public static void runTaskAsynchronously(Runnable task) {
        getFoliaLib().getScheduler().runAsync((wrappedTask) -> task.run());
    }
}
