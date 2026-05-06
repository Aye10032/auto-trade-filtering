package com.aye10032.autotradefiltering.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** C2S: request stored future trades for preview in the merchant screen. */
public record RequestFutureTradesPayload(UUID villagerUuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestFutureTradesPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("auto-trade-filtering", "request_future_trades"));

    public static final StreamCodec<ByteBuf, RequestFutureTradesPayload> CODEC =
            UUIDUtil.STREAM_CODEC.map(RequestFutureTradesPayload::new, RequestFutureTradesPayload::villagerUuid);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
