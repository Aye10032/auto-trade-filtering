package com.aye10032.autotradefiltering.client.mixin;

import com.aye10032.autotradefiltering.client.LibrarianTradeOverlay;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void atf$tickLibrarianTradeOverlay(CallbackInfo ci) {
        LibrarianTradeOverlay.tick();
    }
}
