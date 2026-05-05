package com.aye10032.autotradefiltering.server;

import com.aye10032.autotradefiltering.network.RefreshResultPayload;
import com.aye10032.autotradefiltering.network.TradeFilter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TradeRefreshHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoTradeFiltering");
    private static final int MAX_ATTEMPTS_LIMIT = 10_000;
    private static final double MAX_DISTANCE_SQ = 64.0;
    private static final String ENCHANTED_BOOK_ID = "minecraft:enchanted_book";

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

        int limit = Math.max(1, Math.min(maxAttempts, MAX_ATTEMPTS_LIMIT));
        LOGGER.info("[ATF] 开始为玩家 {} 刷新，目标 {} 个，请求 {} 次，实际最多 {} 次",
                player.getName().getString(), targets.size(), maxAttempts, limit);

        ((FilteredVillager) villager).atf$setFutureTradeData(null);
        for (int i = 1; i <= limit; i++) {
            FullCareerRoll roll = buildFullCareerRoll(level, villager);
            LOGGER.debug("[ATF] 第 {} 次刷新，当前等级 {} 条，完整职业池 {} 条",
                    i, roll.visibleOffers().size(), roll.fullCareerOffers().size());

            if (allTargetsMatched(roll.fullCareerOffers(), targets)) {
                LOGGER.info("[ATF] 第 {} 次刷新命中所有目标！当前等级 {} 条，完整职业池 {} 条", i, roll.visibleOffers().size(), roll.fullCareerOffers().size());
                villager.setOffers(roll.visibleOffers());
                ((FilteredVillager) villager).atf$setFutureTradeData(new FutureTradeData(roll.futureOffers()));
                sendResult(player, true, i, "success");
                return;
            }

            villager.setOffers(roll.visibleOffers());
        }

        ((FilteredVillager) villager).atf$setFutureTradeData(null);
        LOGGER.info("[ATF] 已达最大尝试次数 {}，未找到目标", limit);
        sendResult(player, false, limit, "max_attempts");
    }

    /**
     * 检查本次刷新的交易列表是否包含所有目标（每个目标至少有一条交易命中）
     */
    private static boolean allTargetsMatched(MerchantOffers offers, List<TradeFilter.TradeTarget> targets) {
        Map<String, List<TradeFilter.TradeTarget>> equipmentEnchantTargets = new LinkedHashMap<>();
        List<TradeFilter.TradeTarget> independentTargets = new ArrayList<>();

        for (TradeFilter.TradeTarget target : targets) {
            if (isEquipmentEnchantmentTarget(target)) {
                equipmentEnchantTargets.computeIfAbsent(target.itemId(), ignored -> new ArrayList<>()).add(target);
            } else {
                independentTargets.add(target);
            }
        }

        for (List<TradeFilter.TradeTarget> sameItemTargets : equipmentEnchantTargets.values()) {
            if (sameItemTargets.size() == 1) {
                independentTargets.add(sameItemTargets.getFirst());
            } else if (!hasSingleOfferMatchingAllTargets(offers, sameItemTargets)) {
                return false;
            }
        }

        for (TradeFilter.TradeTarget target : independentTargets) {
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

    private static boolean isEquipmentEnchantmentTarget(TradeFilter.TradeTarget target) {
        return target.hasEnchantmentFilter() && !target.itemId().equals(ENCHANTED_BOOK_ID);
    }

    private static boolean hasSingleOfferMatchingAllTargets(MerchantOffers offers, List<TradeFilter.TradeTarget> targets) {
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            boolean allMatched = true;
            for (TradeFilter.TradeTarget target : targets) {
                if (!matchesTarget(result, target)) {
                    allMatched = false;
                    break;
                }
            }
            if (allMatched) return true;
        }
        return false;
    }

    private static boolean matchesTarget(ItemStack result, TradeFilter.TradeTarget target) {
        String resultItemId = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
        if (!resultItemId.equals(target.itemId())) return false;

        if (target.hasPotionFilter() && !matchesPotion(result, target.potionId())) return false;

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

    private static boolean matchesPotion(ItemStack result, String potionId) {
        PotionContents contents = result.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        return contents.potion()
                .flatMap(holder -> holder.unwrapKey())
                .map(key -> key.identifier().toString().equals(potionId))
                .orElse(false);
    }

    private static void sendResult(ServerPlayer player, boolean success, int attempts, String message) {
        ServerPlayNetworking.send(player, new RefreshResultPayload(success, attempts, message));
    }

    private static FullCareerRoll buildFullCareerRoll(ServerLevel level, Villager villager) {
        VillagerData originalData = villager.getVillagerData();
        int currentLevel = originalData.level();
        VillagerProfession profession = originalData.profession().value();
        MerchantOffers visibleOffers = generateOffersForLevel(level, villager, profession, currentLevel);
        List<MerchantOffers> futureOffers = new ArrayList<>();

        for (int tradeLevel = currentLevel + 1; tradeLevel <= VillagerData.MAX_VILLAGER_LEVEL; tradeLevel++) {
            futureOffers.add(generateOffersForLevel(level, villager, profession, tradeLevel));
        }

        MerchantOffers fullOffers = visibleOffers.copy();
        for (MerchantOffers offers : futureOffers) {
            fullOffers.addAll(offers);
        }
        return new FullCareerRoll(visibleOffers, futureOffers, fullOffers);
    }

    private static MerchantOffers generateOffersForLevel(ServerLevel level, Villager villager, VillagerProfession profession, int tradeLevel) {
        MerchantOffers offers = new MerchantOffers();
        ResourceKey<TradeSet> tradeSet = profession.getTrades(tradeLevel);
        if (tradeSet != null) {
            villager.addOffersFromTradeSet(level, offers, tradeSet);
        }
        return offers;
    }

    private static boolean isProfessionLocked(Villager villager) {
        if (villager.getVillagerXp() > 0) return true;

        MerchantOffers offers = villager.getOffers();
        if (offers == null || offers.isEmpty()) return false;
        for (MerchantOffer offer : offers) {
            if (offer.getUses() > 0) return true;
        }
        return false;
    }

    private record FullCareerRoll(MerchantOffers visibleOffers, List<MerchantOffers> futureOffers, MerchantOffers fullCareerOffers) {
    }
}
