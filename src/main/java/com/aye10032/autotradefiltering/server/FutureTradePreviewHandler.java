package com.aye10032.autotradefiltering.server;

import com.aye10032.autotradefiltering.network.FutureTradesPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.UUID;

public final class FutureTradePreviewHandler {

    private static final double MAX_DISTANCE_SQ = 64.0;

    private FutureTradePreviewHandler() {}

    public static void handle(ServerPlayer player, UUID villagerUuid) {
        MerchantOffers offers = new MerchantOffers();
        ServerLevel level = (ServerLevel) player.level();
        Entity entity = level.getEntity(villagerUuid);

        if (entity instanceof Villager villager
                && player.distanceToSqr(villager) <= MAX_DISTANCE_SQ
                && !isProfessionLocked(villager)) {
            FutureTradeData data = ((FilteredVillager) villager).atf$getFutureTradeData();
            if (data != null && !data.isEmpty()) {
                offers = data.combinedOffers();
            }
        }

        ServerPlayNetworking.send(player, new FutureTradesPayload(villagerUuid, offers));
    }

    public static void send(ServerPlayer player, Villager villager) {
        FutureTradeData data = ((FilteredVillager) villager).atf$getFutureTradeData();
        MerchantOffers offers = data == null || data.isEmpty() ? new MerchantOffers() : data.combinedOffers();
        ServerPlayNetworking.send(player, new FutureTradesPayload(villager.getUUID(), offers));
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
}
