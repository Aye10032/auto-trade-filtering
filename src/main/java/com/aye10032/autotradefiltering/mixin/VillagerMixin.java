package com.aye10032.autotradefiltering.mixin;

import com.aye10032.autotradefiltering.server.FilteredVillager;
import com.aye10032.autotradefiltering.server.FutureTradeData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager implements ReputationEventHandler, VillagerDataHolder, FilteredVillager {

    @Unique
    private FutureTradeData atf$futureTradeData;

    protected VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void atf$saveFutureTradeData(ValueOutput output, CallbackInfo ci) {
        if (atf$futureTradeData != null && !atf$futureTradeData.isEmpty()) {
            atf$futureTradeData.write(output);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void atf$readFutureTradeData(ValueInput input, CallbackInfo ci) {
        atf$futureTradeData = FutureTradeData.read(input).orElse(null);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void atf$clearFutureTradesWhenOffersReset(CallbackInfo ci) {
        if (this.offers == null) {
            atf$futureTradeData = null;
        }
    }

    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    private void atf$appendStoredFutureTrades(ServerLevel level, CallbackInfo ci) {
        if (this.offers == null || this.offers.isEmpty()) {
            atf$futureTradeData = null;
            return;
        }

        if (atf$futureTradeData == null) return;
        Optional<MerchantOffers> nextOffers = atf$futureTradeData.popNextLevelOffers();
        if (nextOffers.isEmpty()) {
            atf$futureTradeData = null;
            return;
        }

        this.offers.addAll(nextOffers.get());
        if (atf$futureTradeData.isEmpty()) {
            atf$futureTradeData = null;
        }
        ci.cancel();
    }

    @Override
    public void atf$setFutureTradeData(FutureTradeData data) {
        this.atf$futureTradeData = data;
    }

    @Override
    public FutureTradeData atf$getFutureTradeData() {
        return atf$futureTradeData;
    }
}
