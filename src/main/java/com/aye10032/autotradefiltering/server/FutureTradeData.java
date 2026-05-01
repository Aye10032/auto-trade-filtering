package com.aye10032.autotradefiltering.server;

import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FutureTradeData {

    private static final String SAVE_KEY = "AutoTradeFilteringFutureOffers";

    private final List<MerchantOffers> futureOffers;

    public FutureTradeData(List<MerchantOffers> futureOffers) {
        this.futureOffers = new ArrayList<>(futureOffers);
    }

    public static Optional<FutureTradeData> read(ValueInput input) {
        return input.read(SAVE_KEY, MerchantOffers.CODEC.listOf())
                .map(FutureTradeData::new);
    }

    public void write(ValueOutput output) {
        if (!futureOffers.isEmpty()) {
            output.store(SAVE_KEY, MerchantOffers.CODEC.listOf(), futureOffers);
        }
    }

    public boolean isEmpty() {
        return futureOffers.isEmpty();
    }

    public Optional<MerchantOffers> popNextLevelOffers() {
        if (futureOffers.isEmpty()) return Optional.empty();
        return Optional.of(futureOffers.removeFirst());
    }

    public MerchantOffers combinedOffers() {
        MerchantOffers combined = new MerchantOffers();
        for (MerchantOffers offers : futureOffers) {
            combined.addAll(offers);
        }
        return combined;
    }
}
