package com.aye10032.autotradefiltering.server;

import com.aye10032.autotradefiltering.network.LibrarianTradesPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LibrarianTradeInfoHandler {

    private static final double MAX_DISTANCE_SQ = 64.0;

    private LibrarianTradeInfoHandler() {
    }

    public static void handle(net.minecraft.server.level.ServerPlayer player, UUID villagerUuid) {
        Entity entity = player.level().getEntity(villagerUuid);
        if (!(entity instanceof Villager villager)
                || !villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)
                || player.distanceToSqr(villager) > MAX_DISTANCE_SQ) {
            ServerPlayNetworking.send(player, new LibrarianTradesPayload(villagerUuid, List.of()));
            return;
        }

        ServerPlayNetworking.send(player, new LibrarianTradesPayload(villagerUuid, collectBookTrades(villager.getOffers())));
    }

    private static List<LibrarianTradesPayload.Entry> collectBookTrades(MerchantOffers offers) {
        List<LibrarianTradesPayload.Entry> entries = new ArrayList<>();
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            if (result.getItem() != Items.ENCHANTED_BOOK) continue;

            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(result);
            for (var entry : enchantments.entrySet()) {
                Component name = Enchantment.getFullname(entry.getKey(), entry.getIntValue());
                entries.add(new LibrarianTradesPayload.Entry(name, emeraldCost(offer), bookCost(offer)));
            }
        }
        return entries;
    }

    private static int emeraldCost(MerchantOffer offer) {
        return itemCount(offer.getCostA(), Items.EMERALD) + itemCount(offer.getCostB(), Items.EMERALD);
    }

    private static int bookCost(MerchantOffer offer) {
        return itemCount(offer.getCostA(), Items.BOOK) + itemCount(offer.getCostB(), Items.BOOK);
    }

    private static int itemCount(ItemStack stack, net.minecraft.world.item.Item item) {
        return stack.getItem() == item ? stack.getCount() : 0;
    }
}
