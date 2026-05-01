package com.aye10032.autotradefiltering;

import com.aye10032.autotradefiltering.network.ModPackets;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoTradeFiltering implements ModInitializer {
	public static final String MOD_ID = "auto-trade-filtering";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[ATF] AutoTradeFiltering 初始化");
		ModPackets.register();
		LOGGER.info("[ATF] 网络包注册完成");
	}
}
