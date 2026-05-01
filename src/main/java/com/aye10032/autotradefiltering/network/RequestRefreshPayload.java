package com.aye10032.autotradefiltering.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** C2S：客户端请求服务端刷新指定村民的交易 */
public record RequestRefreshPayload(UUID villagerUuid, TradeFilter filter, int maxAttempts)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestRefreshPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("auto-trade-filtering", "request_refresh"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestRefreshPayload> CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, RequestRefreshPayload::villagerUuid,
                    TradeFilter.STREAM_CODEC, RequestRefreshPayload::filter,
                    ByteBufCodecs.VAR_INT, RequestRefreshPayload::maxAttempts,
                    RequestRefreshPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
