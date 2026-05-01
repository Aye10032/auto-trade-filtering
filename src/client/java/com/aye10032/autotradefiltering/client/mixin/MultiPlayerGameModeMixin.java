package com.aye10032.autotradefiltering.client.mixin;

import com.aye10032.autotradefiltering.client.gui.TradeFilterScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "interact", at = @At("HEAD"))
    private void atf$captureVillagerUUID(
            Player player, Entity target, EntityHitResult hitResult, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (target instanceof Villager villager) {
            TradeFilterScreen.lastInteractedVillagerUUID = villager.getUUID();
        }
    }
}
