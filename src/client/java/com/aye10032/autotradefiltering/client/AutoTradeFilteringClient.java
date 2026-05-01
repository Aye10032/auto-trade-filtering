package com.aye10032.autotradefiltering.client;

import com.aye10032.autotradefiltering.AutoTradeFiltering;
import com.aye10032.autotradefiltering.network.RefreshResultPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class AutoTradeFilteringClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		AutoTradeFiltering.LOGGER.info("[ATF] 客户端初始化，注册 S2C 接收器");

		ClientPlayNetworking.registerGlobalReceiver(RefreshResultPayload.TYPE, (payload, context) ->
				context.client().execute(() -> showResultToast(payload))
		);
		LibrarianTradeOverlay.registerReceivers();
	}

	private static void showResultToast(RefreshResultPayload payload) {
		Minecraft mc = Minecraft.getInstance();
		Component title;
		Component desc;

		if (payload.success()) {
			title = Component.translatable("msg.auto-trade-filtering.success_title");
			desc = Component.translatable("msg.auto-trade-filtering.success", payload.attempts());
			if (mc.player != null) {
				mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
			}
		} else {
			title = Component.translatable("msg.auto-trade-filtering.fail_title");
			desc = Component.translatable("msg.auto-trade-filtering.fail_" + payload.message(), payload.attempts());
		}

		SystemToast.add(mc.getToastManager(), SystemToast.SystemToastId.NARRATOR_TOGGLE, title, desc);
	}
}
