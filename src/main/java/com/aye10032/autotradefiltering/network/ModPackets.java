package com.aye10032.autotradefiltering.network;

import com.aye10032.autotradefiltering.server.TradeRefreshHandler;
import com.aye10032.autotradefiltering.server.LibrarianTradeInfoHandler;
import com.aye10032.autotradefiltering.server.FutureTradePreviewHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModPackets {

    private ModPackets() {}

    /**
     * 在公共入口（AutoTradeFiltering.onInitialize）调用。
     * 注册包类型（服务端/客户端两侧均需）以及服务端接收器。
     */
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(RequestRefreshPayload.TYPE, RequestRefreshPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestLibrarianTradesPayload.TYPE, RequestLibrarianTradesPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestFutureTradesPayload.TYPE, RequestFutureTradesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RefreshResultPayload.TYPE, RefreshResultPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LibrarianTradesPayload.TYPE, LibrarianTradesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FutureTradesPayload.TYPE, FutureTradesPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestRefreshPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        TradeRefreshHandler.handle(
                                context.player(),
                                payload.villagerUuid(),
                                payload.filter(),
                                payload.maxAttempts()
                        )
                )
        );

        ServerPlayNetworking.registerGlobalReceiver(RequestLibrarianTradesPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        LibrarianTradeInfoHandler.handle(context.player(), payload.villagerUuid())
                )
        );

        ServerPlayNetworking.registerGlobalReceiver(RequestFutureTradesPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        FutureTradePreviewHandler.handle(context.player(), payload.villagerUuid())
                )
        );
    }
}
