package com.aye10032.autotradefiltering.client.mixin;

import com.aye10032.autotradefiltering.client.gui.TradeFilterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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

        // 仅对真实村民显示
        if (TradeFilterScreen.lastInteractedVillagerUUID == null) return;
        ResourceKey<VillagerProfession> profession = getLastInteractedVillagerProfession();
        if (profession == null) return;

        int btnX = (width - 276) / 2 + 110;
        int btnY = (height - 166) / 2 + 25;

        addRenderableWidget(Button.builder(
                Component.literal("F"),
                btn -> Minecraft.getInstance().setScreen(new TradeFilterScreen(menu, self, profession)))
                .bounds(btnX, btnY, 15, 15)
                .build());
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
