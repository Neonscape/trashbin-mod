package io.github.whiteviera.trashbin.client;

import io.github.whiteviera.trashbin.menu.TrashBinMenu;
import io.github.whiteviera.trashbin.platform.VersionPlatform;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class TrashBinScreen extends AbstractContainerScreen<TrashBinMenu> {
    private static final ResourceLocation CHEST = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private static final ResourceLocation BAR = new ResourceLocation("trashbin", "textures/gui/experience_bar.png");
    private Button clear;
    private Button extract;
    public TrashBinScreen(TrashBinMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageHeight = 222; inventoryLabelY = 128;
    }
    @Override protected void init() {
        super.init();
        clear = addRenderableWidget(Button.builder(VersionPlatform.translatable("gui.trashbin.clear"), b -> send(TrashBinMenu.CLEAR))
                .bounds(leftPos + 8, topPos + 106, 48, 20).build());
        extract = addRenderableWidget(Button.builder(VersionPlatform.translatable("gui.trashbin.extract"), b -> send(TrashBinMenu.EXTRACT))
                .bounds(leftPos + 58, topPos + 106, 48, 20).build());
        addRenderableWidget(Button.builder(VersionPlatform.translatable("gui.trashbin.redstone"), b -> send(TrashBinMenu.CYCLE_REDSTONE))
                .bounds(leftPos + 108, topPos + 106, 62, 20).build());
    }
    private void send(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override protected void containerTick() {
        super.containerTick(); clear.active = menu.occupiedSlots() > 0; extract.active = menu.experiencePoints() > 0;
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(CHEST, leftPos, topPos, 0, 0, 176, 71);
        for (int i = 0; i < 5; i++) graphics.blit(CHEST, leftPos, topPos + 71 + 11 * i, 0, 4, 176, 11);
        graphics.blit(CHEST, leftPos, topPos + 126, 0, 126, 176, 96);
        graphics.blit(BAR, leftPos + 8, topPos + 92, 0, 0, 160, 8, 160, 32);
        int fill = ScreenContent.barWidth(menu);
        if (fill > 0) graphics.blit(BAR, leftPos + 9, topPos + 93, 0, 8, fill, 6, 160, 32);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component xp = ScreenContent.experience(menu);
        float scale = Math.min(1F, 146F / Math.max(1, font.width(xp)));
        graphics.pose().pushPose(); graphics.pose().translate(8, 79, 0); graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, xp, 0, 0, 0x404040, false); graphics.pose().popPose();
        graphics.fill(162, 80, 167, 85, menu.automationEnabled() ? 0xff6d936c : 0xffa55b51);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY);
        Component tip = ScreenContent.tooltip(menu, mouseX - leftPos, mouseY - topPos);
        if (tip != null) graphics.renderTooltip(font, font.split(tip, 240), mouseX, mouseY);
    }
}
