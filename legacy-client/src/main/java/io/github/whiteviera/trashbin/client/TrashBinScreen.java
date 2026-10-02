package io.github.whiteviera.trashbin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.whiteviera.trashbin.menu.TrashBinMenu;
import io.github.whiteviera.trashbin.platform.VersionPlatform;
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
        clear = addRenderableWidget(new Button(leftPos + 8, topPos + 106, 48, 20,
                VersionPlatform.translatable("gui.trashbin.clear"), b -> send(TrashBinMenu.CLEAR)));
        extract = addRenderableWidget(new Button(leftPos + 58, topPos + 106, 48, 20,
                VersionPlatform.translatable("gui.trashbin.extract"), b -> send(TrashBinMenu.EXTRACT)));
        addRenderableWidget(new Button(leftPos + 108, topPos + 106, 62, 20,
                VersionPlatform.translatable("gui.trashbin.redstone"), b -> send(TrashBinMenu.CYCLE_REDSTONE)));
    }
    private void send(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override protected void containerTick() {
        super.containerTick(); clear.active = menu.occupiedSlots() > 0; extract.active = menu.experiencePoints() > 0;
    }
    @Override protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1); RenderSystem.setShaderTexture(0, CHEST);
        blit(pose, leftPos, topPos, 0, 0, 176, 71);
        // Reuse the resource pack's blank chest panel, slot frames, borders, and buttons.
        for (int i = 0; i < 5; i++) blit(pose, leftPos, topPos + 71 + 11 * i, 0, 4, 176, 11);
        blit(pose, leftPos, topPos + 126, 0, 126, 176, 96);
        RenderSystem.setShaderTexture(0, BAR);
        blit(pose, leftPos + 8, topPos + 92, 0, 0, 160, 8, 160, 32);
        int fill = ScreenContent.barWidth(menu);
        if (fill > 0) blit(pose, leftPos + 9, topPos + 93, 0, 8, fill, 6, 160, 32);
    }
    @Override protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        super.renderLabels(pose, mouseX, mouseY);
        Component xp = ScreenContent.experience(menu);
        float scale = Math.min(1F, 146F / Math.max(1, font.width(xp)));
        pose.pushPose(); pose.translate(8, 79, 0); pose.scale(scale, scale, 1);
        font.draw(pose, xp, 0, 0, 0x404040); pose.popPose();
        fill(pose, 162, 80, 167, 85, menu.automationEnabled() ? 0xff6d936c : 0xffa55b51);
    }
    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose); super.render(pose, mouseX, mouseY, partialTick); renderTooltip(pose, mouseX, mouseY);
        Component tip = ScreenContent.tooltip(menu, mouseX - leftPos, mouseY - topPos);
        if (tip != null) renderTooltip(pose, font.split(tip, 240), mouseX, mouseY);
    }
}
