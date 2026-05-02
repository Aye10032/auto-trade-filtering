package com.aye10032.autotradefiltering.client;

import com.aye10032.autotradefiltering.network.LibrarianTradesPayload;
import com.aye10032.autotradefiltering.network.RequestLibrarianTradesPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LibrarianTradeOverlay {

    private static final int REQUEST_INTERVAL_TICKS = 20;
    private static final int CACHE_TTL_TICKS = 100;
    private static final int MAX_VISIBLE_ENTRIES = 8;
    private static final double MAX_DISTANCE_SQ = 64.0;

    private static final Map<UUID, CachedTrades> CACHE = new HashMap<>();
    private static final Map<UUID, Integer> LAST_REQUEST_TICKS = new HashMap<>();
    private static boolean enabled = true;

    private LibrarianTradeOverlay() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(LibrarianTradesPayload.TYPE, (payload, context) ->
                context.client().execute(() -> receive(payload))
        );
    }

    public static void tick() {
        if (!enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        int now = currentTick();
        List<Villager> villagers = mc.level.getEntities(
                EntityType.VILLAGER,
                mc.player.getBoundingBox().inflate(8.0),
                villager -> villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)
                        && mc.player.distanceToSqr(villager) <= MAX_DISTANCE_SQ
        );
        for (Villager villager : villagers) {
            UUID uuid = villager.getUUID();
            int lastRequestTick = LAST_REQUEST_TICKS.getOrDefault(uuid, -REQUEST_INTERVAL_TICKS);
            if (now - lastRequestTick >= REQUEST_INTERVAL_TICKS || isCacheExpired(uuid)) {
                ClientPlayNetworking.send(new RequestLibrarianTradesPayload(uuid));
                LAST_REQUEST_TICKS.put(uuid, now);
            }
        }
    }

    public static Component tradeNameTag(Entity entity) {
        List<Component> lines = tradeNameTagLines(entity);
        if (lines.isEmpty()) return null;
        return lines.getFirst();
    }

    private static List<Component> tradeNameTagLines(Entity entity) {
        if (!enabled) return List.of();

        UUID uuid = entity.getUUID();

        CachedTrades cached = CACHE.get(uuid);
        if (cached == null || cached.entries().isEmpty() || isCacheExpired(uuid) || !isNearbyLibrarian(entity)) return List.of();

        List<Component> lines = new ArrayList<>();
        int shown = Math.min(cached.entries().size(), MAX_VISIBLE_ENTRIES);
        for (int i = 0; i < shown; i++) {
            LibrarianTradesPayload.Entry entry = cached.entries().get(i);
            lines.add(formatEntry(entry));
        }
        int hidden = cached.entries().size() - shown;
        if (hidden > 0) {
            lines.add(Component.translatable("overlay.auto-trade-filtering.more", hidden).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    public static void submitLabels(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector collector) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || levelRenderState.cameraRenderState == null) return;

        CameraRenderState camera = levelRenderState.cameraRenderState;
        Vec3 cameraPos = camera.pos;
        List<Villager> villagers = mc.level.getEntities(
                EntityType.VILLAGER,
                mc.player.getBoundingBox().inflate(8.0),
                LibrarianTradeOverlay::isNearbyLibrarian
        );

        for (Villager villager : villagers) {
            List<Component> lines = tradeNameTagLines(villager);
            if (lines.isEmpty()) continue;

            Vec3 pos = villager.getPosition(1.0F);
            double x = pos.x() - cameraPos.x();
            double y = pos.y() - cameraPos.y();
            double z = pos.z() - cameraPos.z();
            double distanceToCameraSq = cameraPos.distanceToSqr(pos);

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            int topLineOffset = -(lines.size() - 1) * 10;
            for (int i = 0; i < lines.size(); i++) {
                collector.submitNameTag(
                        poseStack,
                        new Vec3(0.0, villager.getBbHeight() + 0.3, 0.0),
                        topLineOffset + i * 10,
                        lines.get(i),
                        true,
                        0xF000F0,
                        distanceToCameraSq,
                        camera
                );
            }
            poseStack.popPose();
        }
    }

    private static void receive(LibrarianTradesPayload payload) {
        CACHE.put(payload.villagerUuid(), new CachedTrades(List.copyOf(payload.entries()), currentTick()));
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        LibrarianTradeOverlay.enabled = enabled;
        if (!enabled) {
            CACHE.clear();
            LAST_REQUEST_TICKS.clear();
        }
    }

    private static MutableComponent formatEntry(LibrarianTradesPayload.Entry entry) {
        return Component.empty()
                .append(entry.enchantmentName().copy().withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" "))
                .append(Component.literal(Integer.toString(entry.emeraldCost())).withStyle(ChatFormatting.GREEN));
    }

    private static boolean isNearbyLibrarian(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null
                && entity instanceof Villager villager
                && villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)
                && mc.player.distanceToSqr(villager) <= MAX_DISTANCE_SQ;
    }

    private static boolean isCacheExpired(UUID uuid) {
        CachedTrades cached = CACHE.get(uuid);
        return cached == null || currentTick() - cached.receivedAtTick() > CACHE_TTL_TICKS;
    }

    private static int currentTick() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? 0 : mc.player.tickCount;
    }

    private record CachedTrades(List<LibrarianTradesPayload.Entry> entries, int receivedAtTick) {
    }
}
