package com.aye10032.autotradefiltering;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoTradeFiltering implements ModInitializer {
	public static final String MOD_ID = "auto-trade-filtering";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[ATF] AutoTradeFiltering 初始化，Access Widener 已加载");

		// 调试：验证 Access Widener 是否成功暴露 Villager.updateTrades
		// 若编译通过说明 AW 配置正确；运行时日志可在 logs/latest.log 中查看
		try {
			var method = net.minecraft.world.entity.npc.villager.Villager.class
					.getDeclaredMethod("updateTrades", net.minecraft.server.level.ServerLevel.class);
			LOGGER.info("[ATF] AW 验证成功：updateTrades 可访问，modifier={}", method.getModifiers());
		} catch (NoSuchMethodException e) {
			LOGGER.error("[ATF] AW 验证失败：找不到 updateTrades 方法", e);
		}
	}
}