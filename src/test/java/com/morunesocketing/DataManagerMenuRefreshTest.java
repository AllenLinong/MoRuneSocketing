package com.morunesocketing;

import com.morunesocketing.util.SchedulerUtils;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.mockito.Mockito.*;

class DataManagerMenuRefreshTest {
    @Test
    void successRateChangeSchedulesInventoryAccessOnPlayerThread() {
        MoRuneSocketing plugin = mock(MoRuneSocketing.class);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        DataManager manager = new DataManager(plugin);
        manager.addOpenMenuPlayer(player);
        clearInvocations(player);
        try (var scheduler = mockStatic(SchedulerUtils.class); var menus = mockStatic(RuneManager.class)) {
            manager.setCurrentSuccessRate(70);
            ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
            scheduler.verify(() -> SchedulerUtils.runTaskForEntity(eq(player), callback.capture()));
            menus.verifyNoInteractions();
            verifyNoInteractions(player);
            callback.getValue().run();
            menus.verify(() -> RuneManager.refreshMenu(player));
        }
    }
}
