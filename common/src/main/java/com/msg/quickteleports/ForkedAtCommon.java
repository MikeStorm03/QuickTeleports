package com.msg.quickteleports;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.msg.quickteleports.memory.PlayerData;
import com.msg.quickteleports.memory.PreviousPosition;
import com.msg.quickteleports.memory.TeleportRequest;
import com.msg.quickteleports.util.HereTeleport;
import com.msg.quickteleports.util.TextFormatting;

import dev.itsmeow.quickteleports.QuickTeleportsMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ForkedAtCommon extends QuickTeleportsMod {

    public static HashMap<ServerPlayer, Queue<TeleportRequest>> tps = new HashMap<>();

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // tpahere
        dispatcher.register(Commands.literal("tpahere").requires(source -> source.isPlayer()).then(Commands.argument("target", GameProfileArgument.gameProfile()).executes(command -> {

            CommandSourceStack sourceStack = command.getSource();
            ServerPlayer player = sourceStack.getPlayerOrException();
            MinecraftServer server = player.level().getServer();
            Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(command, "target");

            if(profiles.size() > 1) {
                sourceStack.sendFailure(TextFormatting.shortText("Chỉ gửi đến một đứa thôi!", TextFormatting.red()));
                return 0;
            }
            NameAndId profile = QuickTeleportsMod.getFirstProfile(profiles);
            if(!QuickTeleportsMod.isGameProfileOnline(server, profile)) {
                sourceStack.sendFailure(TextFormatting.shortText("Nó offline rồi!", TextFormatting.red()));
                return 0;
            }
            if(profile.id().equals(player.getGameProfile().id())) {
                sourceStack.sendFailure(TextFormatting.shortText("Không dịch chuyển đến chỗ mình được đâu!", TextFormatting.red()));
                return 0;
            }
            String sourceName = player.getName().getString();
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(profile.id());

            HereTeleport tp = new HereTeleport(sourceName, targetPlayer.getName().getString());

            Queue<TeleportRequest> queue = tps.computeIfAbsent(targetPlayer, k -> new ConcurrentLinkedQueue<>());
            queue.add(new TeleportRequest(tp, getTeleportTimeout() * 20));

            
            targetPlayer.createCommandSourceStack().sendSuccess(
                    () -> TextFormatting.longText(Maps.newLinkedHashMap(ImmutableMap.of(
                                                sourceName, TextFormatting.green(),
                                                "đã gửi bạn lời mời dịch chuyển đến chỗ họ. Nhập ", TextFormatting.gold(),
                                                "/tpaccept", TextFormatting.yellow(),
                                                " để đồng ý hoặc ", TextFormatting.gold(),
                                                "/tpadeny", TextFormatting.yellow(),
                                                " để từ chối.\n", TextFormatting.gold(),
                                                "ACCEPT", TextFormatting.button("tpaccept " + player.getPlainTextName()),
                                                " || ", TextFormatting.white(),
                                                "DENY", TextFormatting.button("tpadeny" + player.getPlainTextName())
                ))), false);

            sourceStack.sendSuccess(
                    () -> TextFormatting.longText(Maps.newLinkedHashMap(ImmutableMap.of(
                                            "Đã gửi ", TextFormatting.gold(),
                                                targetPlayer.getPlainTextName(), TextFormatting.green(),
                                                " yêu cầu dịch chuyển đến chỗ bạn.", TextFormatting.gold()
                ))), false);
            return 1;
        })));

        // back
        dispatcher.register(Commands.literal("back").requires(source -> source.isPlayer()).executes(command -> {
            CommandSourceStack sourceStack = command.getSource();
            ServerPlayer player = sourceStack.getPlayerOrException();
            MinecraftServer server = player.level().getServer();
            PreviousPosition previousPos = PlayerData.getPreviousPosition(player);
            if (previousPos == null) {
                sourceStack.sendFailure(TextFormatting.shortText("Không có địa điểm trước để dịch chuyển!", TextFormatting.red()));
                return 0;
            }
            PlayerData.setPreviousPosition(player, new PreviousPosition(player.getX(), player.getY(), player.getZ(), player.level().dimension()));

            player.teleportTo(server.getLevel(previousPos.getDimension()),
                            previousPos.getX(),
                            previousPos.getY(),
                            previousPos.getZ(),
                            Set.of(),
                            player.getYRot(),
                            player.getXRot(),
                            true);
            return 1;
        }));
    }
}