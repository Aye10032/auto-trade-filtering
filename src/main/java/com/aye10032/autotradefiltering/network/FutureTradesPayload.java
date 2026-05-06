package com.aye10032.autotradefiltering.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.UUID;

/** S2C: stored future trades to preview in the merchant screen. */
public record FutureTradesPayload(UUID villagerUuid, MerchantOffers offers) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FutureTradesPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("auto-trade-filtering", "future_trades"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FutureTradesPayload> CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, FutureTradesPayload::villagerUuid,
                    MerchantOffers.STREAM_CODEC, FutureTradesPayload::offers,
                    FutureTradesPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
