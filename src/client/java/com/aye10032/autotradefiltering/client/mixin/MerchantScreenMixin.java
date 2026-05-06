package com.aye10032.autotradefiltering.client.mixin;

import com.aye10032.autotradefiltering.client.FutureTradePreviewCache;
import com.aye10032.autotradefiltering.client.gui.TradeFilterScreen;
import com.aye10032.autotradefiltering.network.RequestFutureTradesPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends Screen {

    @Unique
    private Button atf$filterButton;
    @Unique
    private int atf$previewScrollOff;
    @Unique
    private boolean atf$draggingPreviewScrollbar;
    @Unique
    private static final Identifier ATF_TRADE_ARROW_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow");

    protected MerchantScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void atf$addFilterButton(CallbackInfo ci) {
        MerchantScreen self = (MerchantScreen) (Object) this;
        MerchantMenu menu = self.getMenu();
        atf$previewScrollOff = 0;
        atf$draggingPreviewScrollbar = false;

        // 仅对真实村民显示
        if (TradeFilterScreen.lastInteractedVillagerUUID == null) return;
        if (TradeFilterScreen.hasUsedTrades(menu)) return;

        ClientPlayNetworking.send(new RequestFutureTradesPayload(TradeFilterScreen.lastInteractedVillagerUUID));

        ResourceKey<VillagerProfession> profession = getLastInteractedVillagerProfession();
        if (profession == null) return;

        int btnX = (width - 276) / 2 + 110;
        int btnY = (height - 166) / 2 + 25;

        atf$filterButton = addRenderableWidget(Button.builder(
                Component.literal("F"),
                btn -> {
                    if (TradeFilterScreen.hasUsedTrades(menu)) {
                        atf$refreshFilterButton(menu);
                        return;
                    }
                    Minecraft.getInstance().setScreen(new TradeFilterScreen(menu, self, profession));
                })
                .bounds(btnX, btnY, 15, 15)
                .build());
    }

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void atf$refreshFilterButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        atf$refreshFilterButton(((MerchantScreen) (Object) this).getMenu());
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void atf$mouseScrolledFutureTradePreview(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        MerchantMenu menu = ((MerchantScreen) (Object) this).getMenu();
        UUID villagerUuid = TradeFilterScreen.lastInteractedVillagerUUID;
        if (villagerUuid == null || TradeFilterScreen.hasUsedTrades(menu)) return;

        MerchantOffers futureOffers = FutureTradePreviewCache.get(villagerUuid);
        if (futureOffers == null || futureOffers.isEmpty()) return;

        int visibleCount = menu.getOffers() == null ? 0 : menu.getOffers().size();
        int freeRows = Math.max(0, 7 - visibleCount);
        int maxScroll = Math.max(0, futureOffers.size() - freeRows);
        if (freeRows <= 0 || maxScroll <= 0 || !atf$isMouseOverTradeList(mouseX, mouseY)) return;

        int oldScroll = atf$previewScrollOff;
        atf$previewScrollOff = Math.max(0, Math.min(maxScroll, atf$previewScrollOff - (int) Math.signum(scrollY)));
        if (oldScroll != atf$previewScrollOff) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void atf$mouseClickedFutureTradePreview(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0 || !atf$canScrollFutureTradePreview()) return;
        if (!atf$isMouseOverPreviewScrollbar(event.x(), event.y())) return;

        atf$draggingPreviewScrollbar = true;
        atf$updatePreviewScrollFromMouse(event.y());
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void atf$mouseDraggedFutureTradePreview(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (!atf$draggingPreviewScrollbar) return;

        atf$updatePreviewScrollFromMouse(event.y());
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void atf$mouseReleasedFutureTradePreview(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        atf$draggingPreviewScrollbar = false;
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void atf$extractFutureTradePreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MerchantScreen self = (MerchantScreen) (Object) this;
        MerchantMenu menu = self.getMenu();
        UUID villagerUuid = TradeFilterScreen.lastInteractedVillagerUUID;
        if (villagerUuid == null) return;

        if (TradeFilterScreen.hasUsedTrades(menu)) {
            FutureTradePreviewCache.clear(villagerUuid);
            return;
        }

        MerchantOffers futureOffers = FutureTradePreviewCache.get(villagerUuid);
        if (futureOffers == null || futureOffers.isEmpty()) return;

        MerchantOffers visibleOffers = menu.getOffers();
        int visibleCount = visibleOffers == null ? 0 : visibleOffers.size();
        int freeRows = Math.max(0, 7 - visibleCount);
        if (freeRows <= 0) return;

        int left = (width - 276) / 2;
        int top = (height - 166) / 2;
        int startRow = Math.min(visibleCount, 7);
        int count = Math.min(freeRows, futureOffers.size());
        atf$previewScrollOff = Math.min(atf$previewScrollOff, Math.max(0, futureOffers.size() - freeRows));

        for (int i = 0; i < count; i++) {
            atf$extractFutureOffer(graphics, futureOffers.get(atf$previewScrollOff + i), left + 10, top + 19 + (startRow + i) * 20, mouseX, mouseY);
        }
        atf$extractFuturePreviewScrollbar(graphics, menu, futureOffers, left, top);
    }

    @Unique
    private void atf$refreshFilterButton(MerchantMenu menu) {
        if (atf$filterButton == null) return;

        boolean hasUsedTrades = TradeFilterScreen.hasUsedTrades(menu);
        atf$filterButton.visible = !hasUsedTrades;
        atf$filterButton.active = !hasUsedTrades;
    }

    @Unique
    private void atf$extractFutureOffer(GuiGraphicsExtractor graphics, MerchantOffer offer, int x, int y, int mouseX, int mouseY) {
        int rowColor = 0x90202020;
        int borderColor = 0x803C3C3C;
        graphics.fill(x - 5, y - 2, x + 83, y + 18, rowColor);
        graphics.outline(x - 5, y - 2, 88, 20, borderColor);

        atf$extractItem(graphics, offer.getCostA(), x, y);
        if (!offer.getCostB().isEmpty()) {
            atf$extractItem(graphics, offer.getCostB(), x + 35, y);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ATF_TRADE_ARROW_SPRITE, x + 55, y + 3, 10, 9);
        atf$extractItem(graphics, offer.getResult(), x + 68, y);

        graphics.fill(x - 5, y - 2, x + 83, y + 18, 0x66000000);
        atf$extractPreviewTooltip(graphics, offer, x, y, mouseX, mouseY);
    }

    @Unique
    private void atf$extractItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        graphics.fakeItem(stack, x, y);
        graphics.itemDecorations(font, stack, x, y);
    }

    @Unique
    private void atf$extractPreviewTooltip(GuiGraphicsExtractor graphics, MerchantOffer offer, int x, int y, int mouseX, int mouseY) {
        if (mouseY < y || mouseY >= y + 16) return;

        if (mouseX >= x && mouseX < x + 20) {
            graphics.setTooltipForNextFrame(font, offer.getCostA(), mouseX, mouseY);
        } else if (mouseX > x + 30 && mouseX < x + 50 && !offer.getCostB().isEmpty()) {
            graphics.setTooltipForNextFrame(font, offer.getCostB(), mouseX, mouseY);
        } else if (mouseX > x + 65 && mouseX < x + 85) {
            graphics.setTooltipForNextFrame(font, offer.getResult(), mouseX, mouseY);
        }
    }

    @Unique
    private boolean atf$isMouseOverTradeList(double mouseX, double mouseY) {
        int left = (width - 276) / 2;
        int top = (height - 166) / 2;
        return mouseX >= left + 5 && mouseX < left + 94
                && mouseY >= top + 18 && mouseY < top + 158;
    }

    @Unique
    private boolean atf$isMouseOverPreviewScrollbar(double mouseX, double mouseY) {
        int left = (width - 276) / 2;
        int top = (height - 166) / 2;
        return mouseX >= left + 94 && mouseX < left + 100
                && mouseY >= top + 18 && mouseY < top + 157;
    }

    @Unique
    private boolean atf$canScrollFutureTradePreview() {
        UUID villagerUuid = TradeFilterScreen.lastInteractedVillagerUUID;
        if (villagerUuid == null) return false;

        MerchantMenu menu = ((MerchantScreen) (Object) this).getMenu();
        if (TradeFilterScreen.hasUsedTrades(menu)) return false;

        MerchantOffers futureOffers = FutureTradePreviewCache.get(villagerUuid);
        return futureOffers != null && atf$previewMaxScroll(menu, futureOffers) > 0;
    }

    @Unique
    private void atf$updatePreviewScrollFromMouse(double mouseY) {
        UUID villagerUuid = TradeFilterScreen.lastInteractedVillagerUUID;
        if (villagerUuid == null) return;

        MerchantMenu menu = ((MerchantScreen) (Object) this).getMenu();
        MerchantOffers futureOffers = FutureTradePreviewCache.get(villagerUuid);
        if (futureOffers == null) return;

        int maxScroll = atf$previewMaxScroll(menu, futureOffers);
        if (maxScroll <= 0) {
            atf$previewScrollOff = 0;
            return;
        }

        int top = (height - 166) / 2;
        int trackY = top + 18;
        int trackH = 139;
        int thumbH = atf$previewThumbHeight(menu, futureOffers);
        double ratio = (mouseY - trackY - thumbH / 2.0) / Math.max(1, trackH - thumbH);
        atf$previewScrollOff = Math.max(0, Math.min(maxScroll, (int) Math.round(ratio * maxScroll)));
    }

    @Unique
    private void atf$extractFuturePreviewScrollbar(GuiGraphicsExtractor graphics, MerchantMenu menu, MerchantOffers futureOffers, int left, int top) {
        int maxScroll = atf$previewMaxScroll(menu, futureOffers);
        if (maxScroll <= 0) return;

        int trackX = left + 94;
        int trackY = top + 18;
        int trackH = 139;
        int thumbH = atf$previewThumbHeight(menu, futureOffers);
        int thumbY = trackY + (trackH - thumbH) * atf$previewScrollOff / maxScroll;
        graphics.fill(trackX, trackY, trackX + 6, trackY + trackH, 0x80202020);
        graphics.fill(trackX + 1, thumbY, trackX + 5, thumbY + thumbH, atf$draggingPreviewScrollbar ? 0xFFB0B0B0 : 0xFF808080);
    }

    @Unique
    private int atf$previewMaxScroll(MerchantMenu menu, MerchantOffers futureOffers) {
        int visibleCount = menu.getOffers() == null ? 0 : menu.getOffers().size();
        int freeRows = Math.max(0, 7 - visibleCount);
        return Math.max(0, futureOffers.size() - freeRows);
    }

    @Unique
    private int atf$previewThumbHeight(MerchantMenu menu, MerchantOffers futureOffers) {
        int visibleCount = menu.getOffers() == null ? 0 : menu.getOffers().size();
        int freeRows = Math.max(1, 7 - visibleCount);
        return Math.max(18, 139 * freeRows / Math.max(freeRows, futureOffers.size()));
    }

    @Unique
    private ResourceKey<VillagerProfession> getLastInteractedVillagerProfession() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof Villager villager
                    && villager.getUUID().equals(TradeFilterScreen.lastInteractedVillagerUUID)) {
                return villager.getVillagerData().profession().unwrapKey().orElse(null);
            }
        }
        return null;
    }
}
