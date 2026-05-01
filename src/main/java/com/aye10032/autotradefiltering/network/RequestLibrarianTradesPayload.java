package com.aye10032.autotradefiltering.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** C2S: request the enchanted-book trades for a looked-at librarian. */
public record RequestLibrarianTradesPayload(UUID villagerUuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestLibrarianTradesPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("auto-trade-filtering", "request_librarian_trades"));

    public static final StreamCodec<ByteBuf, RequestLibrarianTradesPayload> CODEC =
            UUIDUtil.STREAM_CODEC.map(RequestLibrarianTradesPayload::new, RequestLibrarianTradesPayload::villagerUuid);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
