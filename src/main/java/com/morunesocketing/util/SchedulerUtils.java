package com.morunesocketing.util;

import com.morunesocketing.MoRuneSocketing;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 调度器工具类
 * 兼容 Paper、Spigot、Folia 等服务端
 * 使用反射避免编译时依赖Folia API
 */
public class SchedulerUtils {
    
    private static Boolean foliaDetected = null;
    
    /**
     * 检测是否为Folia服务端
     */
    public static boolean isFolia() {
        if (foliaDetected != null) {
            return foliaDetected;
        }
        
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            foliaDetected = true;
        } catch (ClassNotFoundException e) {
            foliaDetected = false;
        }
        
        return foliaDetected;
    }
    
    /**
     * 在主线程执行任务
     */
    public static void runTask(Runnable task) {
        if (isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("execute", JavaPlugin.class, Runnable.class)
                    .invoke(scheduler, MoRuneSocketing.getInstance(), task);
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(MoRuneSocketing.getInstance(), task);
            }
        } else {
            Bukkit.getScheduler().runTask(MoRuneSocketing.getInstance(), task);
        }
    }
    
    /**
     * 延迟执行任务
     */
    public static void runTaskLater(Runnable task, long delayTicks) {
        if (isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("runDelayed", JavaPlugin.class, Runnable.class, long.class)
                    .invoke(scheduler, MoRuneSocketing.getInstance(), task, delayTicks);
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskLater(MoRuneSocketing.getInstance(), task, delayTicks);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(MoRuneSocketing.getInstance(), task, delayTicks);
        }
    }
    
    /**
     * 定时执行任务（全局）
     */
    public static void runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("runAtFixedRate", JavaPlugin.class, Runnable.class, long.class, long.class)
                    .invoke(scheduler, MoRuneSocketing.getInstance(), task, delayTicks, periodTicks);
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskTimer(MoRuneSocketing.getInstance(), task, delayTicks, periodTicks);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(MoRuneSocketing.getInstance(), task, delayTicks, periodTicks);
        }
    }
    
    /**
     * 在实体所在区域执行任务（Folia专用）
     */
    public static void runTaskForEntity(Entity entity, Runnable task) {
        if (isFolia()) {
            try {
                Method getScheduler = Entity.class.getMethod("getScheduler");
                Object scheduler = getScheduler.invoke(entity);
                scheduler.getClass().getMethod("run", JavaPlugin.class, Runnable.class, Runnable.class)
                    .invoke(scheduler, MoRuneSocketing.getInstance(), task, null);
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(MoRuneSocketing.getInstance(), task);
            }
        } else {
            Bukkit.getScheduler().runTask(MoRuneSocketing.getInstance(), task);
        }
    }
    
    /**
     * 在指定位置执行任务（Folia专用）
     */
    public static void runTaskAtLocation(Location location, Runnable task) {
        if (isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("execute", JavaPlugin.class, Location.class, Runnable.class)
                    .invoke(scheduler, MoRuneSocketing.getInstance(), location, task);
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(MoRuneSocketing.getInstance(), task);
            }
        } else {
            Bukkit.getScheduler().runTask(MoRuneSocketing.getInstance(), task);
        }
    }
    
    /**
     * 异步执行任务
     */
    public static void runTaskAsynchronously(Runnable task) {
        if (isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                // Folia的runNow接受Consumer<ScheduledTask>
                scheduler.getClass().getMethod("runNow", JavaPlugin.class, java.util.function.Consumer.class)
                    .invoke(scheduler, MoRuneSocketing.getInstance(), (java.util.function.Consumer<Object>) t -> {
                        try {
                            task.run();
                        } catch (Exception e) {
                            MoRuneSocketing.getInstance().getLogger().warning("异步任务执行异常: " + e.getMessage());
                        }
                    });
            } catch (Exception e) {
                // 回退到标准异步执行
                Bukkit.getScheduler().runTaskAsynchronously(MoRuneSocketing.getInstance(), task);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(MoRuneSocketing.getInstance(), task);
        }
    }
}
