package com.aye10032.autotradefiltering.client.gui;

import com.aye10032.autotradefiltering.network.RequestRefreshPayload;
import com.aye10032.autotradefiltering.network.TradeFilter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TradeFilterScreen extends Screen {

    /** 由 MultiPlayerGameModeMixin 在右键村民时写入 */
    public static UUID lastInteractedVillagerUUID = null;

    private static final int PANEL_W = 260;
    private static final int PANEL_H = 240;
    private static final int OFFER_BTN_H = 20;
    private static final int OFFER_BTN_GAP = 2;
    private static final int MAX_VISIBLE_OFFERS = 7;
    private static final int MAX_SELECTIONS = 2;

    private final MerchantMenu menu;
    private final Screen parent;

    /** 选中的交易下标，最多 MAX_SELECTIONS 个 */
    private final Set<Integer> selectedIndices = new LinkedHashSet<>();

    private EditBox maxAttemptsBox;
    private Button startButton;

    private final Button[] offerButtons = new Button[MAX_VISIBLE_OFFERS];
    private int scrollOffset = 0;
    private int totalOffers = 0;

    public TradeFilterScreen(MerchantMenu menu, Screen parent) {
        super(Component.translatable("gui.auto-trade-filtering.title"));
        this.menu = menu;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        MerchantOffers offers = menu.getOffers();
        totalOffers = (offers == null) ? 0 : offers.size();

        // 交易列表按钮（点击切换选中状态）
        for (int i = 0; i < MAX_VISIBLE_OFFERS; i++) {
            final int slotIdx = i;
            offerButtons[i] = addRenderableWidget(Button.builder(
                    Component.literal(""),
                    btn -> toggleOffer(scrollOffset + slotIdx))
                    .bounds(left + 4, top + 24 + i * (OFFER_BTN_H + OFFER_BTN_GAP), PANEL_W - 28, OFFER_BTN_H)
                    .build());
        }

        // 滚动按钮
        if (totalOffers > MAX_VISIBLE_OFFERS) {
            addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
                if (scrollOffset > 0) { scrollOffset--; refreshOfferButtons(); }
            }).bounds(left + PANEL_W - 22, top + 24, 18, 18).build());

            addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
                if (scrollOffset + MAX_VISIBLE_OFFERS < totalOffers) { scrollOffset++; refreshOfferButtons(); }
            }).bounds(left + PANEL_W - 22, top + 24 + (MAX_VISIBLE_OFFERS - 1) * (OFFER_BTN_H + OFFER_BTN_GAP), 18, 18).build());
        }

        refreshOfferButtons();

        // 最大尝试次数
        int inputY = top + 24 + MAX_VISIBLE_OFFERS * (OFFER_BTN_H + OFFER_BTN_GAP) + 8;
        maxAttemptsBox = addRenderableWidget(new EditBox(
                font, left + 120, inputY, 50, 16,
                Component.translatable("gui.auto-trade-filtering.max_attempts")));
        maxAttemptsBox.setValue("100");
        maxAttemptsBox.setMaxLength(3);

        // 开始刷新
        startButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.auto-trade-filtering.start"),
                btn -> sendRefreshRequest())
                .bounds(left + (PANEL_W - 80) / 2, top + PANEL_H - 26, 80, 20)
                .build());
        startButton.active = false;

        // 取消
        addRenderableWidget(Button.builder(
                Component.translatable("gui.auto-trade-filtering.cancel"),
                btn -> onClose())
                .bounds(left + PANEL_W - 54, top + 2, 52, 14)
                .build());
    }

    private void refreshOfferButtons() {
        MerchantOffers offers = menu.getOffers();
        for (int i = 0; i < MAX_VISIBLE_OFFERS; i++) {
            int realIdx = scrollOffset + i;
            if (offers != null && realIdx < offers.size()) {
                MerchantOffer offer = offers.get(realIdx);
                ItemStack result = offer.getResult();
                boolean selected = selectedIndices.contains(realIdx);
                String label = buildOfferLabel(result, selected, offer.isOutOfStock());
                offerButtons[i].setMessage(Component.literal(label));
                offerButtons[i].visible = true;
                // 已达上限时，未选中项变灰（不可点）
                offerButtons[i].active = selected || selectedIndices.size() < MAX_SELECTIONS;
            } else {
                offerButtons[i].visible = false;
            }
        }
    }

    private String buildOfferLabel(ItemStack stack, boolean selected, boolean outOfStock) {
        String prefix = selected ? "[✓] " : "[ ] ";
        String suffix = outOfStock ? " (缺货)" : "";
        String itemName = stack.getHoverName().getString();
        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (!enchants.keySet().isEmpty()) {
            var holder = enchants.keySet().iterator().next();
            int level = enchants.getLevel(holder);
            String enchName = holder.value().description().getString();
            return prefix + itemName + " [" + enchName + " " + level + "]" + suffix;
        }
        return prefix + itemName + suffix;
    }

    private void toggleOffer(int realIdx) {
        MerchantOffers offers = menu.getOffers();
        if (offers == null || realIdx >= offers.size()) return;

        if (selectedIndices.contains(realIdx)) {
            selectedIndices.remove(realIdx);
        } else if (selectedIndices.size() < MAX_SELECTIONS) {
            selectedIndices.add(realIdx);
        }

        startButton.active = !selectedIndices.isEmpty();
        refreshOfferButtons();
    }

    private void sendRefreshRequest() {
        if (lastInteractedVillagerUUID == null) return;

        MerchantOffers offers = menu.getOffers();
        if (offers == null) return;

        int maxAttempts = 100;
        try { maxAttempts = Integer.parseInt(maxAttemptsBox.getValue()); } catch (NumberFormatException ignored) {}
        maxAttempts = Math.max(1, Math.min(200, maxAttempts));

        List<TradeFilter.TradeTarget> targets = new ArrayList<>();
        for (int idx : selectedIndices) {
            if (idx >= offers.size()) continue;
            MerchantOffer offer = offers.get(idx);
            ItemStack result = offer.getResult();

            String itemId = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
            String enchId = "";
            int enchLevel = 0;

            ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(result);
            if (!enchants.keySet().isEmpty()) {
                var holder = enchants.keySet().iterator().next();
                enchId = holder.unwrapKey()
                        .map(k -> k.identifier().toString())
                        .orElse("");
                enchLevel = enchants.getLevel(holder);
            }

            targets.add(new TradeFilter.TradeTarget(itemId, enchId, enchLevel));
        }

        if (targets.isEmpty()) return;

        ClientPlayNetworking.send(new RequestRefreshPayload(
                lastInteractedVillagerUUID,
                new TradeFilter(targets),
                maxAttempts));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);

        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        // 背景面板
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xC0101010);
        graphics.outline(left, top, left + PANEL_W, top + PANEL_H, 0xFFAAAAAA);

        // 标题
        graphics.centeredText(font, title, width / 2, top + 8, 0xFFFFFF);

        // 提示：最多选几个
        String hint = "（最多选 " + MAX_SELECTIONS + " 个，已选 " + selectedIndices.size() + "）";
        graphics.centeredText(font, hint, width / 2, top + 17, 0xAAAAAA);

        // 最大次数标签
        int inputY = top + 24 + MAX_VISIBLE_OFFERS * (OFFER_BTN_H + OFFER_BTN_GAP) + 8;
        graphics.text(font, Component.translatable("gui.auto-trade-filtering.max_attempts"),
                left + 8, inputY + 4, 0xCCCCCC);

        // 渲染子组件
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
