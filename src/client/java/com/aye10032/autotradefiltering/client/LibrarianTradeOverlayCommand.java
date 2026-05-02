package com.aye10032.autotradefiltering.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class LibrarianTradeOverlayCommand {

    private LibrarianTradeOverlayCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("atf")
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("overlay")
                                .executes(context -> report(context.getSource()))
                                .then(RequiredArgumentBuilder.<FabricClientCommandSource, Boolean>argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setEnabled(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "enabled")
                                        ))
                                )
                        )
                )
        );
    }

    private static int report(FabricClientCommandSource source) {
        source.sendFeedback(Component.translatable(
                LibrarianTradeOverlay.isEnabled()
                        ? "cmd.auto-trade-filtering.overlay.enabled"
                        : "cmd.auto-trade-filtering.overlay.disabled"
        ));
        return 1;
    }

    private static int setEnabled(FabricClientCommandSource source, boolean enabled) {
        LibrarianTradeOverlay.setEnabled(enabled);
        return report(source);
    }
}
