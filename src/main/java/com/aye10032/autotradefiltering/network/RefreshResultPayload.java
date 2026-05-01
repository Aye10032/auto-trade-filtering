package com.aye10032.autotradefiltering.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S2C：服务端向客户端通知刷新结果 */
public record RefreshResultPayload(boolean success, int attempts, String message)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RefreshResultPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("auto-trade-filtering", "refresh_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RefreshResultPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, RefreshResultPayload::success,
                    ByteBufCodecs.VAR_INT, RefreshResultPayload::attempts,
                    ByteBufCodecs.STRING_UTF8, RefreshResultPayload::message,
                    RefreshResultPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
