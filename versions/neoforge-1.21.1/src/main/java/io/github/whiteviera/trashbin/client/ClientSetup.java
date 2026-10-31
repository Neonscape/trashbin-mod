package io.github.whiteviera.trashbin.client;

import io.github.whiteviera.trashbin.TrashBinMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = TrashBinMod.ID, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
        event.register(TrashBinMod.BIN_MENU.get(), TrashBinScreen::new);
    }
    private ClientSetup() {}
}
