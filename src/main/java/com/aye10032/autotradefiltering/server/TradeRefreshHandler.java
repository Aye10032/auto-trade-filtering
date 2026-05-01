package com.aye10032.autotradefiltering.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class TradeRefreshHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoTradeFiltering");
    private static final int MAX_ATTEMPTS_LIMIT = 200;
    private static final double MAX_DISTANCE_SQ = 64.0; // 8格

    /**
     * 临时调试入口：不需要网络包，直接在主模块初始化时可调用测试。
     * 正式版将由网络包接收器调用。
     */
    public static void handle(ServerPlayer player, UUID villagerUuid, int maxAttempts) {
        ServerLevel level = (ServerLevel) player.level();
        Entity entity = level.getEntity(villagerUuid);

        if (!(entity instanceof Villager villager)) {
            LOGGER.warn("[ATF] UUID {} 对应的实体不是村民", villagerUuid);
            return;
        }

        // 距离检查（防止跨图作弊）
        double distSq = player.distanceToSqr(villager);
        if (distSq > MAX_DISTANCE_SQ) {
            LOGGER.warn("[ATF] 玩家 {} 距离村民过远 ({} 格²)，拒绝刷新", player.getName().getString(), (int) distSq);
            return;
        }

        // 职业检查
        if (villager.getVillagerData().profession().unwrapKey().isEmpty()) {
            LOGGER.warn("[ATF] 村民没有职业，无法刷新交易");
            return;
        }

        // 职业锁定检查：任意交易已被使用过则职业已锁定
        if (isProfessionLocked(villager)) {
            LOGGER.warn("[ATF] 村民职业已锁定（已发生交易），无法刷新");
            return;
        }

        int limit = Math.min(maxAttempts, MAX_ATTEMPTS_LIMIT);
        LOGGER.info("[ATF] 开始为玩家 {} 刷新村民交易，最多尝试 {} 次", player.getName().getString(), limit);

        for (int i = 1; i <= limit; i++) {
            // 重置交易列表并重新生成
            villager.setOffers(new MerchantOffers());
            villager.updateTrades(level); // Access Widener 暴露的 protected 方法

            MerchantOffers offers = villager.getOffers();
            LOGGER.debug("[ATF] 第 {} 次刷新，共生成 {} 条交易", i, offers.size());

            // TODO: 此处替换为实际 filter 匹配逻辑（网络包实现后）
            // 临时：打印所有交易的输出物，方便调试
            for (MerchantOffer offer : offers) {
                LOGGER.debug("[ATF]   -> {}", offer.getResult().getItem());
            }

            // 临时：始终在第一次刷新后停止，避免无限循环
            LOGGER.info("[ATF] 调试模式：仅执行 1 次刷新（等待筛选逻辑接入）");
            return;
        }

        LOGGER.info("[ATF] 已达最大尝试次数 {}，未找到目标交易", limit);
    }

    private static boolean isProfessionLocked(Villager villager) {
        MerchantOffers offers = villager.getOffers();
        if (offers == null || offers.isEmpty()) return false;
        for (MerchantOffer offer : offers) {
            if (offer.getUses() > 0) return true;
        }
        return false;
    }
}
