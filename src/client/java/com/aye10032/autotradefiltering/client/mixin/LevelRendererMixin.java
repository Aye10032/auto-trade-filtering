package com.aye10032.autotradefiltering.client.mixin;

import com.aye10032.autotradefiltering.client.LibrarianTradeOverlay;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void atf$submitLibrarianTradeLabels(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector collector, CallbackInfo ci) {
        LibrarianTradeOverlay.submitLabels(poseStack, levelRenderState, collector);
    }
}
