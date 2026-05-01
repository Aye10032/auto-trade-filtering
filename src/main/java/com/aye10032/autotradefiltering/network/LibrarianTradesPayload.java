package com.aye10032.autotradefiltering.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/** S2C: enchanted-book trades and prices for a librarian. */
public record LibrarianTradesPayload(UUID villagerUuid, List<Entry> entries) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LibrarianTradesPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("auto-trade-filtering", "librarian_trades"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LibrarianTradesPayload> CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, LibrarianTradesPayload::villagerUuid,
                    Entry.STREAM_CODEC.apply(ByteBufCodecs.list(64)), LibrarianTradesPayload::entries,
                    LibrarianTradesPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(Component enchantmentName, int emeraldCost, int bookCost) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC =
                StreamCodec.composite(
                        ComponentSerialization.TRUSTED_STREAM_CODEC, Entry::enchantmentName,
                        ByteBufCodecs.VAR_INT, Entry::emeraldCost,
                        ByteBufCodecs.VAR_INT, Entry::bookCost,
                        Entry::new
                );
    }
}
