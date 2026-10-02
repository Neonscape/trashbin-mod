package io.github.whiteviera.trashbin.client;

import io.github.whiteviera.trashbin.menu.TrashBinMenu;
import io.github.whiteviera.trashbin.platform.VersionPlatform;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** Shared UI wording and hit boxes; only drawing and button APIs vary between versions. */
public final class ScreenContent {
    public static Component experience(TrashBinMenu menu) {
        return VersionPlatform.translatable("gui.trashbin.experience", menu.experiencePoints(), menu.experienceCapacity());
    }
    public static int barWidth(TrashBinMenu menu) {
        return menu.experienceCapacity() <= 0 ? 0 : (int) Math.min(158,
                ((long) menu.experiencePoints() * 20 + menu.fractionalUnits()) * 158 / ((long) menu.experienceCapacity() * 20));
    }
    public static Component tooltip(TrashBinMenu menu, double x, double y) {
        if (y >= 106 && y < 126) {
            if (x >= 8 && x < 56) return VersionPlatform.translatable("tooltip.trashbin.clear");
            if (x >= 58 && x < 106) return VersionPlatform.translatable("tooltip.trashbin.extract");
            if (x >= 108 && x < 170) return VersionPlatform.translatable("tooltip.trashbin.redstone", VersionPlatform.translatable(menu.mode().translationKey()));
        }
        if (x >= 8 && x < 169 && y >= 76 && y < 101) {
            return VersionPlatform.translatable("tooltip.trashbin.status",
                    VersionPlatform.translatable(menu.automationEnabled() ? "gui.trashbin.enabled" : "gui.trashbin.disabled"),
                    VersionPlatform.translatable("gui.trashbin.fluid." + menu.outputFluidId()),
                    menu.occupiedSlots(), String.format(Locale.ROOT, "%02d", menu.fractionalUnits() * 5));
        }
        return null;
    }
    private ScreenContent() {}
}
