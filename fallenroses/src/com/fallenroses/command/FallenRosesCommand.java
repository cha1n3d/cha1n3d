package com.fallenroses.command;

import com.fallenroses.player.ClassSelection;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.player.Progression;
import com.fallenroses.player.Talent;
import com.fallenroses.technique.Technique;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class FallenRosesCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> technique = Commands.literal("technique")
            .then(Commands.literal("none").executes(c -> modify(c, d -> d.setTechnique(null))));

        for (Technique t : Technique.values()) {
            technique.then(Commands.literal(t.id()).executes(c -> modify(c, d -> d.setTechnique(t))));
        }

        dispatcher.register(
            Commands.literal("fallenroses")
                .requires(source -> source.hasPermission(2))
                .then(technique)
                .then(
                    Commands.literal("xp")
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    Progression.addXp(p, IntegerArgumentType.getInteger(c, "amount"));
                                    return 1;
                                }))
                        )
                )
                .then(Commands.literal("level").then(Commands.argument("level", IntegerArgumentType.integer(1, 50)).executes(c -> modify(c, d -> {
                        int target = IntegerArgumentType.getInteger(c, "level");
                        d.setLevel(target);
                        d.setXp(0);
                        int total = Progression.talentPointsForLevel(target);
                        if (d.spentTalentPoints() > total) {
                            d.refundTalents();
                        }

                        d.setTalentPoints(total - d.spentTalentPoints());
                    }))))
                .then(
                    Commands.literal("points")
                        .then(
                            Commands.argument("count", IntegerArgumentType.integer(0, 1000))
                                .executes(c -> modify(c, d -> d.setTalentPoints(IntegerArgumentType.getInteger(c, "count"))))
                        )
                )
                .then(Commands.literal("talents").then(Commands.literal("all").executes(c -> modify(c, d -> {
                        for (Talent tx : Talent.values()) {
                            if (tx.availableFor(d.getPlayerClass())) {
                                d.setTalentRank(tx, tx.maxRank);
                            }
                        }
                    }))).then(Commands.literal("reset").executes(c -> modify(c, PlayerData::refundTalents))))
                .then(
                    Commands.literal("energy")
                        .then(
                            Commands.argument("value", FloatArgumentType.floatArg(0.0F))
                                .executes(c -> modify(c, d -> d.setEnergy(FloatArgumentType.getFloat(c, "value"))))
                        )
                )
                .then(
                    Commands.literal("fingers")
                        .then(
                            Commands.argument("count", IntegerArgumentType.integer(0, 20))
                                .executes(c -> modify(c, d -> d.setFingers(IntegerArgumentType.getInteger(c, "count"))))
                        )
                )
                .then(Commands.literal("class").then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    PlayerData d = PlayerDataCap.get(p);
                    if (d == null) {
                        return 0;
                    } else {
                        d.resetClass();
                        Progression.applyAttributes(p);
                        ClassSelection.startIntroIfNeeded(p, false);
                        return 1;
                    }
                })))
                .then(Commands.literal("info").executes(c -> modify(c, d -> {
                    })))
        );
    }

    private static int modify(CommandContext<CommandSourceStack> c, Consumer<PlayerData> action) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        PlayerData data = PlayerDataCap.get(player);
        if (data == null) {
            return 0;
        } else {
            action.accept(data);
            Progression.applyAttributes(player);
            data.markUrgent();
            c.getSource().sendSuccess(() -> info(data), false);
            return 1;
        }
    }

    private static Component info(PlayerData d) {
        Technique t = d.getTechnique();
        PlayerClass cls = d.getPlayerClass();
        return Component.translatable(
            "command.fallenroses.info",
            cls == null ? Component.literal("—") : cls.displayName(),
            d.getLevel(),
            d.getXp(),
            Progression.xpForNext(d.getLevel()),
            Math.round(d.getEnergy()),
            Math.round(d.getMax()),
            t == null ? Component.translatable("technique.fallenroses.none") : t.displayName(),
            d.getTalentPoints(),
            d.spentTalentPoints()
        );
    }

    private FallenRosesCommand() {
    }
}
