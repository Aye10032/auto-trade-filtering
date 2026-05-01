package com.aye10032.autotradefiltering.client.mixin;

import com.aye10032.autotradefiltering.client.gui.TradeFilterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends Screen {

    protected MerchantScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void atf$addFilterButton(CallbackInfo ci) {
        MerchantScreen self = (MerchantScreen) (Object) this;
        MerchantMenu menu = self.getMenu();

        // 仅对真实村民显示（UUID 需在右键时由 MultiPlayerGameModeMixin 捕获）
        if (TradeFilterScreen.lastInteractedVillagerUUID == null) return;

        // 按钮放在屏幕右上角，交易界面标题旁
        int btnX = (width - 176) / 2 + 178;
        int btnY = (height - 166) / 2;

        addRenderableWidget(Button.builder(
                Component.translatable("gui.auto-trade-filtering.filter"),
                btn -> Minecraft.getInstance().setScreen(new TradeFilterScreen(menu, self)))
                .bounds(btnX, btnY, 60, 14)
                .build());
    }
}
