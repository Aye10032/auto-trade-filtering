package com.aye10032.autotradefiltering.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * 客户端传给服务端的筛选条件，支持同时要求 1-2 个目标交易。
 * targets 列表中每项代表一个必须出现的交易目标。
 * 服务端要求所有目标在同一次刷新中全部命中。
 */
public record TradeFilter(List<TradeTarget> targets) {

    /**
     * 单个交易目标：itemId 为物品注册名，enchantmentId 为空串表示不筛选附魔，
     * enchantLevel 为 0 表示不限等级。
     */
    public record TradeTarget(String itemId, String enchantmentId, int enchantLevel) {

        public static final StreamCodec<ByteBuf, TradeTarget> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TradeTarget::itemId,
                ByteBufCodecs.STRING_UTF8, TradeTarget::enchantmentId,
                ByteBufCodecs.VAR_INT, TradeTarget::enchantLevel,
                TradeTarget::new
        );

        public boolean hasEnchantmentFilter() {
            return !enchantmentId.isEmpty();
        }
    }

    public static final StreamCodec<ByteBuf, TradeFilter> STREAM_CODEC =
            TradeTarget.STREAM_CODEC.apply(ByteBufCodecs.list(2))
                    .map(TradeFilter::new, TradeFilter::targets);
}
