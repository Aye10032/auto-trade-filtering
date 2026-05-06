package com.aye10032.autotradefiltering.client;

import com.aye10032.autotradefiltering.network.FutureTradesPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FutureTradePreviewCache {

    private static final Map<UUID, MerchantOffers> OFFERS_BY_VILLAGER = new HashMap<>();

    private FutureTradePreviewCache() {}

    public static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(FutureTradesPayload.TYPE, (payload, context) ->
                context.client().execute(() -> set(payload))
        );
    }

    public static MerchantOffers get(UUID villagerUuid) {
        return OFFERS_BY_VILLAGER.get(villagerUuid);
    }

    public static void clear(UUID villagerUuid) {
        OFFERS_BY_VILLAGER.remove(villagerUuid);
    }

    private static void set(FutureTradesPayload payload) {
        if (payload.offers().isEmpty()) {
            clear(payload.villagerUuid());
        } else {
            OFFERS_BY_VILLAGER.put(payload.villagerUuid(), payload.offers());
        }
    }
}
