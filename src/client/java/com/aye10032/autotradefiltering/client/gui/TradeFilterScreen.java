package com.aye10032.autotradefiltering.client.gui;

import com.aye10032.autotradefiltering.network.RequestRefreshPayload;
import com.aye10032.autotradefiltering.network.TradeFilter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TradeFilterScreen extends Screen {

    /** 由 MultiPlayerGameModeMixin 在右键村民时写入 */
    public static UUID lastInteractedVillagerUUID = null;

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 250;
    private static final int TARGET_BTN_H = 20;
    private static final int TARGET_BTN_GAP = 2;
    private static final int MAX_VISIBLE_TARGETS = 7;
    private static final int MAX_SELECTIONS = 2;
    private static final int LIST_TOP_OFFSET = 38;
    private static final int SCROLLBAR_W = 6;

    private final MerchantMenu menu;
    private final Screen parent;
    private final ResourceKey<VillagerProfession> profession;
    private final List<TargetEntry> targets = new ArrayList<>();
    private final Set<Integer> selectedIndices = new LinkedHashSet<>();

    private EditBox maxAttemptsBox;
    private Button startButton;

    private final Button[] targetButtons = new Button[MAX_VISIBLE_TARGETS];
    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;

    public TradeFilterScreen(MerchantMenu menu, Screen parent, ResourceKey<VillagerProfession> profession) {
        super(Component.translatable("gui.auto-trade-filtering.title"));
        this.menu = menu;
        this.parent = parent;
        this.profession = profession;
    }

    @Override
    protected void init() {
        targets.clear();
        targets.addAll(buildTargets());

        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        for (int i = 0; i < MAX_VISIBLE_TARGETS; i++) {
            final int slotIdx = i;
            targetButtons[i] = addRenderableWidget(Button.builder(
                    Component.literal(""),
                    btn -> toggleTarget(scrollOffset + slotIdx))
                    .bounds(left + 10, top + LIST_TOP_OFFSET + i * (TARGET_BTN_H + TARGET_BTN_GAP), PANEL_W - 30, TARGET_BTN_H)
                    .build());
        }

        refreshTargetButtons();

        int inputY = top + LIST_TOP_OFFSET + MAX_VISIBLE_TARGETS * (TARGET_BTN_H + TARGET_BTN_GAP) + 8;
        maxAttemptsBox = addRenderableWidget(new EditBox(
                font, left + (PANEL_W - 54) / 2, inputY, 54, 16,
                Component.translatable("gui.auto-trade-filtering.max_attempts")));
        maxAttemptsBox.setValue("100");
        maxAttemptsBox.setMaxLength(3);

        startButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.auto-trade-filtering.start"),
                btn -> sendRefreshRequest())
                .bounds(left + (PANEL_W - 82) / 2, top + PANEL_H - 28, 82, 20)
                .build());
        startButton.active = false;

        addRenderableWidget(Button.builder(
                Component.translatable("gui.auto-trade-filtering.cancel"),
                btn -> onClose())
                .bounds(left + PANEL_W - 58, top + 8, 48, 16)
                .build());
    }

    private List<TargetEntry> buildTargets() {
        List<TargetEntry> result = new ArrayList<>();
        if (profession == VillagerProfession.LIBRARIAN) {
            addEnchantedBookTargets(result);
        } else if (profession == VillagerProfession.ARMORER) {
            addEnchantedItemTargets(result, List.of(
                    Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS));
        } else if (profession == VillagerProfession.TOOLSMITH) {
            addEnchantedItemTargets(result, List.of(
                    Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_PICKAXE));
        } else if (profession == VillagerProfession.WEAPONSMITH) {
            addEnchantedItemTargets(result, List.of(Items.DIAMOND_AXE, Items.DIAMOND_SWORD));
        } else if (profession == VillagerProfession.MASON) {
            addColorBlockTargets(result, List.of(
                    Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.MAGENTA_TERRACOTTA, Items.LIGHT_BLUE_TERRACOTTA,
                    Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA, Items.PINK_TERRACOTTA, Items.GRAY_TERRACOTTA,
                    Items.LIGHT_GRAY_TERRACOTTA, Items.CYAN_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.BLUE_TERRACOTTA,
                    Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA, Items.RED_TERRACOTTA, Items.BLACK_TERRACOTTA,
                    Items.WHITE_GLAZED_TERRACOTTA, Items.ORANGE_GLAZED_TERRACOTTA, Items.MAGENTA_GLAZED_TERRACOTTA,
                    Items.LIGHT_BLUE_GLAZED_TERRACOTTA, Items.YELLOW_GLAZED_TERRACOTTA, Items.LIME_GLAZED_TERRACOTTA,
                    Items.PINK_GLAZED_TERRACOTTA, Items.GRAY_GLAZED_TERRACOTTA, Items.LIGHT_GRAY_GLAZED_TERRACOTTA,
                    Items.CYAN_GLAZED_TERRACOTTA, Items.PURPLE_GLAZED_TERRACOTTA, Items.BLUE_GLAZED_TERRACOTTA,
                    Items.BROWN_GLAZED_TERRACOTTA, Items.GREEN_GLAZED_TERRACOTTA, Items.RED_GLAZED_TERRACOTTA,
                    Items.BLACK_GLAZED_TERRACOTTA));
        } else if (profession == VillagerProfession.SHEPHERD) {
            addColorBlockTargets(result, List.of(
                    Items.WHITE_WOOL, Items.ORANGE_WOOL, Items.MAGENTA_WOOL, Items.LIGHT_BLUE_WOOL,
                    Items.YELLOW_WOOL, Items.LIME_WOOL, Items.PINK_WOOL, Items.GRAY_WOOL,
                    Items.LIGHT_GRAY_WOOL, Items.CYAN_WOOL, Items.PURPLE_WOOL, Items.BLUE_WOOL,
                    Items.BROWN_WOOL, Items.GREEN_WOOL, Items.RED_WOOL, Items.BLACK_WOOL));
        }
        return result;
    }

    private void addEnchantedBookTargets(List<TargetEntry> result) {
        Registry<Enchantment> registry = enchantmentRegistry();
        if (registry == null) return;
        for (var entry : registry.entrySet()) {
            Enchantment enchantment = entry.getValue();
            int level = enchantment.getMaxLevel();
            result.add(new TargetEntry(Items.ENCHANTED_BOOK, entry.getKey().identifier().toString(), level,
                    Items.ENCHANTED_BOOK.getName(new ItemStack(Items.ENCHANTED_BOOK)).getString()
                            + " - " + enchantment.description().getString() + " " + level));
        }
    }

    private void addEnchantedItemTargets(List<TargetEntry> result, List<Item> items) {
        Registry<Enchantment> registry = enchantmentRegistry();
        if (registry == null) return;
        for (Item item : items) {
            ItemStack stack = new ItemStack(item);
            for (var entry : registry.entrySet()) {
                Enchantment enchantment = entry.getValue();
                if (!enchantment.canEnchant(stack)) continue;
                int level = enchantment.getMaxLevel();
                result.add(new TargetEntry(item, entry.getKey().identifier().toString(), level,
                        stack.getHoverName().getString() + " - " + enchantment.description().getString() + " " + level));
            }
        }
    }

    private void addColorBlockTargets(List<TargetEntry> result, List<Item> items) {
        for (Item item : items) {
            ItemStack stack = new ItemStack(item);
            result.add(new TargetEntry(item, "", 0, stack.getHoverName().getString()));
        }
    }

    private Registry<Enchantment> enchantmentRegistry() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        return minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    }

    private void refreshTargetButtons() {
        for (int i = 0; i < MAX_VISIBLE_TARGETS; i++) {
            int realIdx = scrollOffset + i;
            if (realIdx < targets.size()) {
                TargetEntry target = targets.get(realIdx);
                boolean selected = selectedIndices.contains(realIdx);
                targetButtons[i].setMessage(Component.literal((selected ? "[✓] " : "[ ] ") + target.label()));
                targetButtons[i].visible = true;
                targetButtons[i].active = selected || selectedIndices.size() < MAX_SELECTIONS;
            } else {
                targetButtons[i].visible = false;
            }
        }
    }

    private void toggleTarget(int realIdx) {
        if (realIdx >= targets.size()) return;

        if (selectedIndices.contains(realIdx)) {
            selectedIndices.remove(realIdx);
        } else if (selectedIndices.size() < MAX_SELECTIONS) {
            selectedIndices.add(realIdx);
        }

        startButton.active = !selectedIndices.isEmpty();
        refreshTargetButtons();
    }

    private void sendRefreshRequest() {
        if (lastInteractedVillagerUUID == null) return;

        int maxAttempts = 100;
        try { maxAttempts = Integer.parseInt(maxAttemptsBox.getValue()); } catch (NumberFormatException ignored) {}
        maxAttempts = Math.max(1, Math.min(200, maxAttempts));

        List<TradeFilter.TradeTarget> selectedTargets = new ArrayList<>();
        for (int idx : selectedIndices) {
            if (idx >= targets.size()) continue;
            TargetEntry target = targets.get(idx);
            selectedTargets.add(new TradeFilter.TradeTarget(target.itemId(), target.enchantmentId(), target.enchantLevel()));
        }

        if (selectedTargets.isEmpty()) return;

        ClientPlayNetworking.send(new RequestRefreshPayload(
                lastInteractedVillagerUUID,
                new TradeFilter(selectedTargets),
                maxAttempts));
        onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOverList(mouseX, mouseY) && scrollBy((int) -Math.signum(scrollY))) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isMouseOverScrollbar(event.x(), event.y())) {
            draggingScrollbar = true;
            updateScrollFromMouse(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar) {
            updateScrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        draggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);

        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xC0101010);
        graphics.outline(left, top, PANEL_W, PANEL_H, 0xFFAAAAAA);

        graphics.centeredText(font, title, width / 2, top + 8, 0xFFFFFF);

        String professionName = BuiltInRegistries.VILLAGER_PROFESSION
                .getValue(profession)
                .name()
                .getString();
        graphics.centeredText(font, Component.literal(professionName), width / 2, top + 19, 0xAAAAAA);

        String hint = targets.isEmpty()
                ? Component.translatable("gui.auto-trade-filtering.unsupported_profession").getString()
                : "（最多选 " + MAX_SELECTIONS + " 个，已选 " + selectedIndices.size() + "）";
        graphics.centeredText(font, Component.literal(hint), width / 2, top + 28, 0xAAAAAA);

        if (canScroll()) {
            int trackX = scrollbarX(left);
            int trackY = listTop(top);
            int trackH = listHeight();
            int thumbH = scrollbarThumbHeight();
            int thumbY = scrollbarThumbY(trackY, trackH, thumbH);
            graphics.fill(trackX, trackY, trackX + SCROLLBAR_W, trackY + trackH, 0xFF202020);
            graphics.fill(trackX + 1, thumbY, trackX + SCROLLBAR_W - 1, thumbY + thumbH, 0xFFAAAAAA);
        }

        int inputY = top + LIST_TOP_OFFSET + MAX_VISIBLE_TARGETS * (TARGET_BTN_H + TARGET_BTN_GAP) + 8;
        graphics.text(font, Component.translatable("gui.auto-trade-filtering.max_attempts"),
                left + 8, inputY + 4, 0xCCCCCC);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean scrollBy(int amount) {
        if (!canScroll() || amount == 0) return false;
        int oldOffset = scrollOffset;
        scrollOffset = Math.max(0, Math.min(maxScrollOffset(), scrollOffset + amount));
        if (oldOffset != scrollOffset) {
            refreshTargetButtons();
            return true;
        }
        return false;
    }

    private void updateScrollFromMouse(double mouseY) {
        int top = (height - PANEL_H) / 2;
        int trackY = listTop(top);
        int trackH = listHeight();
        int thumbH = scrollbarThumbHeight();
        double ratio = (mouseY - trackY - thumbH / 2.0) / Math.max(1, trackH - thumbH);
        scrollOffset = Math.max(0, Math.min(maxScrollOffset(), (int) Math.round(ratio * maxScrollOffset())));
        refreshTargetButtons();
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        return mouseX >= left + 8 && mouseX <= left + PANEL_W - 8
                && mouseY >= listTop(top) && mouseY <= listTop(top) + listHeight();
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        if (!canScroll()) return false;
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        return mouseX >= scrollbarX(left) - 2 && mouseX <= scrollbarX(left) + SCROLLBAR_W + 2
                && mouseY >= listTop(top) && mouseY <= listTop(top) + listHeight();
    }

    private boolean canScroll() {
        return targets.size() > MAX_VISIBLE_TARGETS;
    }

    private int maxScrollOffset() {
        return Math.max(0, targets.size() - MAX_VISIBLE_TARGETS);
    }

    private int listTop(int top) {
        return top + LIST_TOP_OFFSET;
    }

    private int listHeight() {
        return MAX_VISIBLE_TARGETS * TARGET_BTN_H + (MAX_VISIBLE_TARGETS - 1) * TARGET_BTN_GAP;
    }

    private int scrollbarX(int left) {
        return left + PANEL_W - 14;
    }

    private int scrollbarThumbHeight() {
        return Math.max(18, listHeight() * MAX_VISIBLE_TARGETS / Math.max(MAX_VISIBLE_TARGETS, targets.size()));
    }

    private int scrollbarThumbY(int trackY, int trackH, int thumbH) {
        if (maxScrollOffset() == 0) return trackY;
        return trackY + (trackH - thumbH) * scrollOffset / maxScrollOffset();
    }

    private record TargetEntry(Item item, String enchantmentId, int enchantLevel, String label) {
        private String itemId() {
            return BuiltInRegistries.ITEM.getKey(item).toString();
        }
    }
}
