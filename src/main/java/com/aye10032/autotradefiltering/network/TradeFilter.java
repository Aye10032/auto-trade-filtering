package com.aye10032.autotradefiltering.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 客户端传给服务端的筛选条件。
 * enchantmentId 为空字符串表示不筛选附魔；enchantLevel 为 0 表示不筛选等级。
 */
public record TradeFilter(String itemId, String enchantmentId, int enchantLevel) {

    public static final StreamCodec<ByteBuf, TradeFilter> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TradeFilter::itemId,
            ByteBufCodecs.STRING_UTF8, TradeFilter::enchantmentId,
            ByteBufCodecs.VAR_INT, TradeFilter::enchantLevel,
            TradeFilter::new
    );

    public boolean hasEnchantmentFilter() {
        return !enchantmentId.isEmpty();
    }
}
