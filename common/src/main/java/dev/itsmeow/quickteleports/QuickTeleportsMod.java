package dev.itsmeow.quickteleports;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.msg.quickteleports.ForkedAtCommon;
import com.msg.quickteleports.memory.PlayerData;
import com.msg.quickteleports.memory.PreviousPosition;
import com.msg.quickteleports.memory.TeleportRequest;
import com.msg.quickteleports.platform.Services;
import com.msg.quickteleports.util.TextFormatting;

import dev.itsmeow.quickteleports.util.Teleport;
import dev.itsmeow.quickteleports.util.ToTeleport;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QuickTeleportsMod {

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // tpa
        dispatcher.register(Commands.literal("tpa").requires(source -> source.isPlayer()).then(Commands.argument("target", GameProfileArgument.gameProfile()).executes(command -> {
            CommandSourceStack sourceStack = command.getSource();
            ServerPlayer player = sourceStack.getPlayerOrException();
            MinecraftServer server = player.level().getServer();
            Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(command, "target");

            if(profiles.size() > 1) {
                sourceStack.sendFailure(TextFormatting.shortText("Chỉ gửi đến một đứa thôi!", TextFormatting.red()));
                return 0;
            }
            NameAndId profile = getFirstProfile(profiles);
            if(!isGameProfileOnline(server, profile)) {
                sourceStack.sendFailure(TextFormatting.shortText("Nó offline rồi!", TextFormatting.red()));
                return 0;
            }
            if(profile.id().equals(player.getGameProfile().id())) {
                sourceStack.sendFailure(TextFormatting.shortText("Không dịch chuyển đến chỗ bản thân được đâu!", TextFormatting.red()));
                return 0;
            }
            String sourceName = player.getName().getString();
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(profile.id());
        

            Queue<TeleportRequest> queue = ForkedAtCommon.tps.computeIfAbsent(targetPlayer, k -> new ConcurrentLinkedQueue<>());
            boolean exists = queue.stream().anyMatch(req -> req.getTeleport().getRequester().equals(sourceName));
            if(!exists) {
                ToTeleport teleport = new ToTeleport(sourceName, targetPlayer.getName().getString());
                queue.add(new TeleportRequest(teleport, getTeleportTimeout() * 20));
            } else {
                sourceStack.sendFailure(TextFormatting.shortText("Bro gửi yêu cầu dịch chuyển rồi!", TextFormatting.red()));
                return 0;
            }

            targetPlayer.createCommandSourceStack().sendSuccess(
                // style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "say Hello!"))
                    () -> TextFormatting.longText(Maps.newLinkedHashMap(ImmutableMap.of(
                                                    sourceName, TextFormatting.green(),
                                                    " muốn dịch chuyển đến chỗ bạn. Nhập ", TextFormatting.gold(),
                                                    "/tpaccept", TextFormatting.yellow(),
                                                    " để đồng ý hoặc ", TextFormatting.gold(),
                                                    "/tpadeny", TextFormatting.yellow(),
                                                    " để từ chối.\n", TextFormatting.gold(),
                                                    "ACCEPT", TextFormatting.button("tpaccept " + player.getPlainTextName()),
                                                    " || ", TextFormatting.white(),
                                                    "DENY", TextFormatting.button("tpadeny " + player.getPlainTextName()))
                    )), false);

            sourceStack.sendSuccess(
                    () -> TextFormatting.longText(Maps.newLinkedHashMap(ImmutableMap.of(
                                            "Yêu cầu dịch chuyển đã được gửi đến ", TextFormatting.gold(),
                                                targetPlayer.getPlainTextName(), TextFormatting.green(),
                                                ".", TextFormatting.gold()
                    ))), false);
            return 1;
        })));

        // tpaccept
        dispatcher.register(Commands.literal("tpaccept").requires(source -> source.isPlayer()).executes(command -> {
            CommandSourceStack sourceStack = command.getSource();
            ServerPlayer player = sourceStack.getPlayerOrException();
            MinecraftServer server = player.level().getServer();

            TeleportRequest req =  QuickTeleportsMod.getSubjectTP(player);
            if(req == null) {
                sourceStack.sendFailure(TextFormatting.shortText("Bạn hiện không có yêu cầu dịch chuyển nào.", TextFormatting.red()));
                return 0;
            }

            Teleport tp = req.getTeleport();
            ServerPlayer playerRequesting = server.getPlayerList().getPlayerByName(tp.getRequester());
            ServerPlayer playerMoving = server.getPlayerList().getPlayerByName(tp.getSubject());

            if(playerMoving == null) {
                sourceStack.sendFailure(TextFormatting.shortText("The player that is teleporting no longer exists!", TextFormatting.red()));
                return 0;
            }

            if(tp instanceof ToTeleport) {
                ServerPlayer holder = playerMoving;
                playerMoving = playerRequesting;
                playerRequesting = holder;
            }

            playerRequesting.createCommandSourceStack().sendSuccess(() -> TextFormatting.shortText("Yêu cầu dịch chuyển đã được đồng ý.", TextFormatting.green()), false);
            playerMoving.createCommandSourceStack().sendSuccess(() -> TextFormatting.shortText(tp instanceof ToTeleport ?
                                                                                "Yêu cầu dịch chuyển của bạn đã được đồng ý." :
                                                                                "Bạn đang được dịch chuyển.", TextFormatting.green()), false);

            PlayerData.setPreviousPosition(playerMoving,
                                        new PreviousPosition(playerMoving.getX(),
                                                            playerMoving.getY(),
                                                            playerMoving.getZ(),
                                                            playerMoving.level().dimension()));
            playerMoving.teleportTo(playerRequesting.level(),
                                    playerRequesting.getX(),
                                    playerRequesting.getY(),
                                    playerRequesting.getZ(),
                                    Set.of(),
                                    playerRequesting.getYRot(),
                                    playerRequesting.getXRot(),
                                    true);
            return 1;

        }).then(Commands.argument("target", GameProfileArgument.gameProfile()).executes(command -> {
            ServerPlayer player = command.getSource().getPlayerOrException();
            MinecraftServer server = player.level().getServer();
        
            Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(command, "target");    
            NameAndId profile = getFirstProfile(profiles);
            if(!isGameProfileOnline(server, profile)) {
                command.getSource().sendFailure(TextFormatting.shortText("Nó offline rồi!", TextFormatting.red()));
                return 0;
            }

            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(profile.id());
            TeleportRequest request = QuickTeleportsMod.getSubjectTP(player, targetPlayer);

            Teleport tp = request.getTeleport();

            ServerPlayer playerRequesting = server.getPlayerList().getPlayerByName(tp.getRequester());
            ServerPlayer playerMoving = server.getPlayerList().getPlayerByName(tp.getSubject());

            if(tp instanceof ToTeleport) {
                ServerPlayer holder = playerMoving;
                playerMoving = playerRequesting;
                playerRequesting = holder;
            }

            playerRequesting.createCommandSourceStack().sendSuccess(() -> TextFormatting.shortText("Yêu cầu dịch chuyển đã được đồng ý.", TextFormatting.green()), false);
            playerMoving.createCommandSourceStack().sendSuccess(() -> TextFormatting.shortText(tp instanceof ToTeleport ?
                                                                                            "Yêu cầu dịch chuyển đã được đồng ý" :
                                                                                            "Bạn đang được dịch chuyển.", TextFormatting.green())
                                                                , false);

            PlayerData.setPreviousPosition(playerMoving, new PreviousPosition(playerMoving.getX(), playerMoving.getY(), playerMoving.getZ(), playerMoving.level().dimension()));
            playerMoving.teleportTo(playerRequesting.level(),
                                    playerRequesting.getX(),
                                    playerRequesting.getY(),
                                    playerRequesting.getZ(),
                                    Set.of(),
                                    playerRequesting.getYRot(),
                                    playerRequesting.getXRot(),
                                    true);
            return 1;
        })));

        // tpadeny
        dispatcher.register(Commands.literal("tpadeny").requires(source -> source.isPlayer()).executes(command -> {
            ServerPlayer player = command.getSource().getPlayerOrException();

            TeleportRequest req = QuickTeleportsMod.getSubjectTP(player);
            if(req == null) {
                command.getSource().sendFailure(TextFormatting.shortText("Bạn không có bất kỳ yêu cầu dịch chuyển nào!", TextFormatting.red()));
                return 0;
            }

            notifyCanceledTP(player.level().getServer(), req.getTeleport());
            return 1;

        }).then(Commands.argument("target", GameProfileArgument.gameProfile()).executes(command -> {

            ServerPlayer player = command.getSource().getPlayerOrException();
            ServerPlayer targetPlayer = player.level().getServer().getPlayerList().getPlayer(getFirstProfile(GameProfileArgument.getGameProfiles(command, "target")).id());

            TeleportRequest req = QuickTeleportsMod.getSubjectTP(player, targetPlayer);
            if(req == null) {
                command.getSource().sendFailure(Component.literal("Bạn không có yêu cầu dịch nào từ " + targetPlayer.getPlainTextName() + ".").withStyle(TextFormatting.red()));
                return 0;
            }
            notifyCanceledTP(player.level().getServer(), req.getTeleport());
            return 1;

        })));

    }

    public static int getTeleportTimeout() {
        return Services.PLATFORM.getTeleportTimeout();
    }

    protected static boolean isGameProfileOnline(MinecraftServer server, NameAndId profile) {
        ServerPlayer player = server.getPlayerList().getPlayer(profile.id());
        if(player != null) {
            return true;
        }
        return false;
    }

    protected static NameAndId getFirstProfile(Collection<NameAndId> profiles) {
        for(NameAndId profile : profiles) {
            return profile;
        }
        return null;
    }

    @Nullable
    public static TeleportRequest getSubjectTP(ServerPlayer player) {
        Queue<TeleportRequest> queue = ForkedAtCommon.tps.get(player);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        return queue.poll();
    }

    @Nullable
    public static TeleportRequest getSubjectTP(ServerPlayer getter, ServerPlayer sender) {
        Queue<TeleportRequest> queue = ForkedAtCommon.tps.get(getter);
        TeleportRequest request = queue.stream()
                                        .filter(req -> req.getTeleport().getRequester().equals(sender.getName().getString()))
                                        .findFirst()
                                        .orElse(null);

        if (request != null) {
            queue.remove(request);
        }

        return request;
    }

    public static void serverTick(MinecraftServer server) {
        for (ServerPlayer player : ForkedAtCommon.tps.keySet()) {
            Queue<TeleportRequest> queue = ForkedAtCommon.tps.get(player);
            Iterator<TeleportRequest> it = queue.iterator();
            while (it.hasNext()) {
                TeleportRequest req = it.next();
                req.decrementTimeout();
                if (req.isExpired()) {
                    it.remove();
                    notifyTimeoutTP(server, req.getTeleport());
                }
            }
        }
    }

    public static void notifyTimeoutTP(MinecraftServer server, Teleport tp) {
        ServerPlayer tper = server.getPlayerList().getPlayerByName(tp.getRequester());
        ServerPlayer target = server.getPlayerList().getPlayerByName(tp.getSubject());
        if(target != null) {
            target.createCommandSourceStack().sendSuccess(
                    () -> TextFormatting.longText(Maps.newLinkedHashMap(ImmutableMap.of(
                                            "Yêu cầu dịch chuyển từ ", TextFormatting.gold(),
                                                tp.getRequester(), TextFormatting.green(),
                                                " đã quá hạn.", TextFormatting.gold()
                ))), false);
        }
        if(tper != null) {
            tper.createCommandSourceStack().sendSuccess(
                    () -> TextFormatting.longText(Maps.newLinkedHashMap(ImmutableMap.of(
                                            "Your request to ", TextFormatting.gold(),
                                                tp.getSubject(), TextFormatting.green(),
                                                " has timed out after not being accepted.", TextFormatting.gold()
                ))), false);
        }
    }

    public static void notifyCanceledTP(MinecraftServer server, Teleport tp) {
        ServerPlayer tper = server.getPlayerList().getPlayerByName(tp.getRequester());
        ServerPlayer target = server.getPlayerList().getPlayerByName(tp.getSubject());
        if(target != null) {
            target.createCommandSourceStack().sendSuccess(
                    () -> TextFormatting.longText(Maps.newLinkedHashMap(ImmutableMap.of(
                                            "Yêu cầu dịch chuyển của ", TextFormatting.gold(),
                                                tp.getRequester(), TextFormatting.green(),
                                                " đã bị từ chối.", TextFormatting.gold()
                ))), false);
        }
        if(tper != null) {
            tper.createCommandSourceStack().sendSuccess(
                    () -> TextFormatting.longText(Maps.newLinkedHashMap(ImmutableMap.of(
                                                "Yêu cầu dịch chuyển đến ",TextFormatting.gold(),
                                                tp.getSubject(), TextFormatting.green(),
                                                " đã bị từ chối.", TextFormatting.gold()
                ))), false);
        }
    }
}
