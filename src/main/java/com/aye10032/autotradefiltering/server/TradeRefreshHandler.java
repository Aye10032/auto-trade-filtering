package com.aye10032.autotradefiltering.server;

import com.aye10032.autotradefiltering.network.RefreshResultPayload;
import com.aye10032.autotradefiltering.network.TradeFilter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class TradeRefreshHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoTradeFiltering");
    private static final int MAX_ATTEMPTS_LIMIT = 200;
    private static final double MAX_DISTANCE_SQ = 64.0;

    public static void handle(ServerPlayer player, UUID villagerUuid, TradeFilter filter, int maxAttempts) {
        ServerLevel level = (ServerLevel) player.level();
        Entity entity = level.getEntity(villagerUuid);

        if (!(entity instanceof Villager villager)) {
            LOGGER.warn("[ATF] UUID {} 对应的实体不是村民", villagerUuid);
            sendResult(player, false, 0, "entity_not_villager");
            return;
        }

        double distSq = player.distanceToSqr(villager);
        if (distSq > MAX_DISTANCE_SQ) {
            LOGGER.warn("[ATF] 玩家 {} 距离村民过远，拒绝刷新", player.getName().getString());
            sendResult(player, false, 0, "too_far");
            return;
        }

        if (villager.getVillagerData().profession().unwrapKey().isEmpty()) {
            LOGGER.warn("[ATF] 村民没有职业，无法刷新");
            sendResult(player, false, 0, "no_profession");
            return;
        }

        if (isProfessionLocked(villager)) {
            LOGGER.warn("[ATF] 村民职业已锁定（已发生交易）");
            sendResult(player, false, 0, "profession_locked");
            return;
        }

        List<TradeFilter.TradeTarget> targets = filter.targets();
        if (targets.isEmpty()) {
            sendResult(player, false, 0, "no_target");
            return;
        }

        int limit = Math.min(maxAttempts, MAX_ATTEMPTS_LIMIT);
        LOGGER.info("[ATF] 开始为玩家 {} 刷新，目标 {} 个，最多 {} 次",
                player.getName().getString(), targets.size(), limit);

        for (int i = 1; i <= limit; i++) {
            villager.setOffers(new MerchantOffers());
            villager.updateTrades(level);

            MerchantOffers offers = villager.getOffers();
            LOGGER.debug("[ATF] 第 {} 次刷新，生成 {} 条交易", i, offers.size());

            if (allTargetsMatched(offers, targets)) {
                LOGGER.info("[ATF] 第 {} 次刷新命中所有目标！", i);
                sendResult(player, true, i, "success");
                return;
            }
        }

        LOGGER.info("[ATF] 已达最大尝试次数 {}，未找到目标", limit);
        sendResult(player, false, limit, "max_attempts");
    }

    /** 检查本次刷新的交易列表是否包含所有目标（每个目标至少有一条交易命中） */
    private static boolean allTargetsMatched(MerchantOffers offers, List<TradeFilter.TradeTarget> targets) {
        for (TradeFilter.TradeTarget target : targets) {
            boolean targetFound = false;
            for (MerchantOffer offer : offers) {
                if (matchesTarget(offer.getResult(), target)) {
                    targetFound = true;
                    break;
                }
            }
            if (!targetFound) return false;
        }
        return true;
    }

    private static boolean matchesTarget(ItemStack result, TradeFilter.TradeTarget target) {
        String resultItemId = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
        if (!resultItemId.equals(target.itemId())) return false;

        if (!target.hasEnchantmentFilter()) return true;

        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(result);
        for (var entry : enchantments.entrySet()) {
            String enchId = entry.getKey().unwrapKey()
                    .map(k -> k.identifier().toString())
                    .orElse("");
            if (enchId.equals(target.enchantmentId())) {
                return target.enchantLevel() == 0 || entry.getIntValue() == target.enchantLevel();
            }
        }
        return false;
    }

    private static void sendResult(ServerPlayer player, boolean success, int attempts, String message) {
        ServerPlayNetworking.send(player, new RefreshResultPayload(success, attempts, message));
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
