package dev.itsmeow.quickteleports;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.itsmeow.quickteleports.memory.PlayerData;
import dev.itsmeow.quickteleports.memory.PreviousPosition;
import dev.itsmeow.quickteleports.memory.TeleportRequest;
import dev.itsmeow.quickteleports.util.HereTeleport;
import dev.itsmeow.quickteleports.util.Teleport;
import dev.itsmeow.quickteleports.util.ToTeleport;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public class QuickTeleportsMod {

    public static final String MOD_ID = "quickteleports";
    public static final String CONFIG_FIELD_NAME = "teleport_request_timeout";
    public static final String CONFIG_FIELD_COMMENT = "Timeout until a teleport request expires, in seconds.";
    public static final int CONFIG_FIELD_VALUE = 30;
    public static final int CONFIG_FIELD_MIN = 0;
    public static final int CONFIG_FIELD_MAX = Integer.MAX_VALUE;

	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static HashMap<ServerPlayer, Queue<TeleportRequest>> tps = new HashMap<>();

    public static class FTC extends TextComponent {

        public FTC(ChatFormatting color, String msg) {
            super(msg);
            this.setStyle(Style.EMPTY.withColor(color));
        }

    
    }
    public static class Button extends TextComponent {

        public Button(String msg, String command) {
            super(msg);
            this.setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
        }

    }

    public static void registerCommands(CommandDispatcher dispatcher) {
        Predicate<CommandSourceStack> isPlayer = source -> {
            try {
                return source.getPlayerOrException() != null;
            } catch(CommandSyntaxException e) {
                return false;
            }
        };
        // tpa
        dispatcher.register(Commands.literal("tpa").requires(isPlayer).then(Commands.argument("target", GameProfileArgument.gameProfile()).executes(command -> {
            ServerPlayer player = command.getSource().getPlayerOrException();
            MinecraftServer server = player.getServer();
            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(command, "target");
            if(profiles.size() > 1) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "Chỉ gửi đến một đứa thôi!"));
                return 0;
            }
            GameProfile profile = getFirstProfile(profiles);
            if(!isGameProfileOnline(server, profile)) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "Nó offline rồi!"));
                return 0;
            }
            if(profile.getId().equals(player.getGameProfile().getId())) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "Không dịch chuyển đến chỗ mình được đâu!"));
                return 0;
            }
            String sourceName = player.getName().getString();
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(profile.getId());

            Queue<TeleportRequest> queue = tps.computeIfAbsent(targetPlayer, k -> new ConcurrentLinkedQueue<>());
            boolean exists = queue.stream().anyMatch(req -> req.getTeleport().getRequester().equals(sourceName));
            if(!exists) {
                ToTeleport teleport = new ToTeleport(sourceName, targetPlayer.getName().getString());
                queue.add(new TeleportRequest(teleport, getTeleportTimeout() * 20));
            } else {
                sendMessage(player.createCommandSourceStack(), false, new FTC(ChatFormatting.RED, "You already have a pending request to this player!"));
                return 0;
            }
            
            sendMessage(targetPlayer.createCommandSourceStack(), true, new FTC(ChatFormatting.GREEN, sourceName),
                        new FTC(ChatFormatting.GOLD, " has requested to teleport to you. Type "), new FTC(ChatFormatting.YELLOW, "/tpaccept"),
                        new FTC(ChatFormatting.GOLD, " to accept or "), new FTC(ChatFormatting.YELLOW, "/tpadeny"), new FTC(ChatFormatting.GOLD, " to deny.\n"),
                        new Button("ACCEPT", "/tpaccept"), new FTC(ChatFormatting.WHITE, " || "), new Button("DENY", "/tpadeny"));
            sendMessage(command.getSource(), true, new FTC(ChatFormatting.GOLD, "Requested to teleport to "), new FTC(ChatFormatting.GREEN, targetPlayer.getName().getString()), new FTC(ChatFormatting.GOLD, "."));
            return 1;
        })));

        // tpaccept
        dispatcher.register(Commands.literal("tpaccept").requires(isPlayer).executes(command -> {
            ServerPlayer player = command.getSource().getPlayerOrException();
            MinecraftServer server = player.getServer();
            Teleport tp = QuickTeleportsMod.getSubjectTP(player).getTeleport();

            if(tp == null) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "You have no pending teleport requests!"));
                return 0;
            }

            ServerPlayer playerRequesting = server.getPlayerList().getPlayerByName(tp.getRequester());
            ServerPlayer playerMoving = server.getPlayerList().getPlayerByName(tp.getSubject());

            if(playerMoving == null) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "The player that is teleporting no longer exists!"));
                return 0;
            }

            if(tp instanceof ToTeleport) {
                ServerPlayer holder = playerMoving;
                playerMoving = playerRequesting;
                playerRequesting = holder;
            }

            sendMessage(playerRequesting.createCommandSourceStack(), true, new FTC(ChatFormatting.GREEN, "Teleport request accepted."));
            sendMessage(playerMoving.createCommandSourceStack(), true, new FTC(ChatFormatting.GREEN, (tp instanceof ToTeleport ? "Your teleport request has been accepted." : "You are now being teleported.")));

            double posX = playerRequesting.getX();
            double posY = playerRequesting.getY();
            double posZ = playerRequesting.getZ();
            PlayerData.setPreviousPosition(playerMoving, new PreviousPosition(playerMoving.getX(), playerMoving.getY(), playerMoving.getZ(), playerMoving.getLevel().dimension()));
            playerMoving.teleportTo(playerRequesting.getLevel(), posX, posY, posZ, playerRequesting.yRot, 0F);
            return 1;

        }).then(Commands.argument("target", GameProfileArgument.gameProfile()).executes(command -> {
            ServerPlayer player = command.getSource().getPlayerOrException();
            MinecraftServer server = player.getServer();
        
            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(command, "target");    
            GameProfile profile = getFirstProfile(profiles);
            if(!isGameProfileOnline(server, profile)) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "Nó offline rồi!"));
                return 0;
            }

            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(profile.getId());
            TeleportRequest request = QuickTeleportsMod.getSubjectTP(player, targetPlayer);

            Teleport tp = request.getTeleport();

            ServerPlayer playerRequesting = server.getPlayerList().getPlayerByName(tp.getRequester());
            ServerPlayer playerMoving = server.getPlayerList().getPlayerByName(tp.getSubject());

            if(tp instanceof ToTeleport) {
                ServerPlayer holder = playerMoving;
                playerMoving = playerRequesting;
                playerRequesting = holder;
            }

            sendMessage(playerRequesting.createCommandSourceStack(), true, new FTC(ChatFormatting.GREEN, "Teleport request accepted."));
            sendMessage(playerMoving.createCommandSourceStack(), true, new FTC(ChatFormatting.GREEN, (tp instanceof ToTeleport ? "Your teleport request has been accepted." : "You are now being teleported.")));

            double posX = playerRequesting.getX();
            double posY = playerRequesting.getY();
            double posZ = playerRequesting.getZ();
            PlayerData.setPreviousPosition(playerMoving, new PreviousPosition(playerMoving.getX(), playerMoving.getY(), playerMoving.getZ(), playerMoving.getLevel().dimension()));
            playerMoving.teleportTo(playerRequesting.getLevel(), posX, posY, posZ, playerRequesting.yRot, 0F);
            return 1;
        })));

        // tpadeny
        dispatcher.register(Commands.literal("tpadeny").requires(isPlayer).executes(command -> {
            ServerPlayer player = command.getSource().getPlayerOrException();
            TeleportRequest req = QuickTeleportsMod.getSubjectTP(player);
            if(req == null) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "You have no pending teleport requests!"));
                return 0;
            }
            Teleport tp = req.getTeleport();
            notifyCanceledTP(player.getServer(), tp);
            return 1;
        }).then(Commands.argument("target", GameProfileArgument.gameProfile()).executes(command -> {
            ServerPlayer player = command.getSource().getPlayerOrException();
            ServerPlayer targetPlayer = player.getServer().getPlayerList().getPlayer(getFirstProfile(GameProfileArgument.getGameProfiles(command, "target")).getId());
            TeleportRequest req = QuickTeleportsMod.getSubjectTP(player, targetPlayer);
            if(req == null) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "You have no pending teleport requests from that player!"));
                return 0;
            }
            Teleport tp = req.getTeleport();
            notifyCanceledTP(player.getServer(), tp);
            return 1;
        })));

        // tpahere
        dispatcher.register(Commands.literal("tpahere").requires(isPlayer).then(Commands.argument("target", GameProfileArgument.gameProfile()).executes(command -> {
            ServerPlayer player = command.getSource().getPlayerOrException();
            MinecraftServer server = player.getServer();
            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(command, "target");
            if(profiles.size() > 1) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "Chỉ gửi đến một đứa thôi!"));
                return 0;
            }
            GameProfile profile = getFirstProfile(profiles);
            if(!isGameProfileOnline(server, profile)) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "Nó offline rồi!"));
                return 0;
            }
            if(profile.getId().equals(player.getGameProfile().getId())) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "Không dịch chuyển đến chỗ mình được đâu!"));
                return 0;
            }
            String sourceName = player.getName().getString();
            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(profile.getId());

            HereTeleport tp = new HereTeleport(sourceName, targetPlayer.getName().getString());

            Queue<TeleportRequest> queue = tps.computeIfAbsent(targetPlayer, k -> new ConcurrentLinkedQueue<>());
            queue.add(new TeleportRequest(tp, getTeleportTimeout() * 20));

            sendMessage(targetPlayer.createCommandSourceStack(), true, new FTC(ChatFormatting.GREEN, sourceName),
                        new FTC(ChatFormatting.GOLD, " has requested that you teleport to them. Type "), new FTC(ChatFormatting.YELLOW, "/tpaccept"),
                        new FTC(ChatFormatting.GOLD, " to accept or "), new FTC(ChatFormatting.YELLOW, "/tpadeny"), new FTC(ChatFormatting.GOLD, " to deny.\n"), 
                        new Button("ACCEPT", "/tpaccept"), new FTC(ChatFormatting.WHITE, " || "), new Button("DENY", "/tpadeny"));
            sendMessage(command.getSource(), true, new FTC(ChatFormatting.GOLD, "Requested "), new FTC(ChatFormatting.GREEN, targetPlayer.getName().getString()), new FTC(ChatFormatting.GOLD, " to teleport to you."));

            return 1;
        })));

        // back
        dispatcher.register(Commands.literal("back").requires(isPlayer).executes(command -> {
            ServerPlayer player = command.getSource().getPlayerOrException();
            MinecraftServer server = player.getServer();
            PreviousPosition previousPos = PlayerData.getPreviousPosition(player);
            if (previousPos == null) {
                sendMessage(command.getSource(), false, new FTC(ChatFormatting.RED, "There is no previous position!"));
                return 0;
            }
            PlayerData.setPreviousPosition(player, new PreviousPosition(player.getX(), player.getY(), player.getZ(), player.getLevel().dimension()));
            player.teleportTo(server.getLevel(previousPos.getDimension()), previousPos.getX(), previousPos.getY(), previousPos.getZ(), player.yRot, 0F);
            return 1;
        }));
    }

    @ExpectPlatform
    public static int getTeleportTimeout() {
        throw new RuntimeException();
    }

    private static boolean isGameProfileOnline(MinecraftServer server, GameProfile profile) {
        ServerPlayer player = server.getPlayerList().getPlayer(profile.getId());
        if(player != null) {
            if(server.getPlayerList().getPlayers().contains(player)) {
                return true;
            }
        }
        return false;
    }

    private static GameProfile getFirstProfile(Collection<GameProfile> profiles) {
        for(GameProfile profile : profiles) {
            return profile;
        }
        return null;
    }

    @Nullable
    public static TeleportRequest getSubjectTP(ServerPlayer player) {
        Queue<TeleportRequest> queue = QuickTeleportsMod.tps.get(player);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        return queue.poll();
    }

    @Nullable
    public static TeleportRequest getSubjectTP(ServerPlayer getter, ServerPlayer sender) {
        Queue<TeleportRequest> queue = QuickTeleportsMod.tps.get(getter);
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
        for (ServerPlayer player : QuickTeleportsMod.tps.keySet()) {
            Queue<TeleportRequest> queue = QuickTeleportsMod.tps.get(player);
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
            sendMessage(target.createCommandSourceStack(), true, new FTC(ChatFormatting.GOLD, "Teleport request from "), new FTC(ChatFormatting.GREEN, tp.getRequester()), new FTC(ChatFormatting.GOLD, " timed out."));
        }
        if(tper != null) {
            sendMessage(tper.createCommandSourceStack(), true, new FTC(ChatFormatting.GOLD, "Your request to "), new FTC(ChatFormatting.GREEN, tp.getSubject()), new FTC(ChatFormatting.GOLD, " has timed out after not being accepted."));
        }
    }

    public static void notifyCanceledTP(MinecraftServer server, Teleport tp) {
        ServerPlayer tper = server.getPlayerList().getPlayerByName(tp.getRequester());
        ServerPlayer target = server.getPlayerList().getPlayerByName(tp.getSubject());
        if(target != null) {
            sendMessage(target.createCommandSourceStack(), true, new FTC(ChatFormatting.GOLD, "Teleport request from "), new FTC(ChatFormatting.GREEN, tp.getRequester()), new FTC(ChatFormatting.GOLD, " has been denied."));
        }
        if(tper != null) {
            sendMessage(tper.createCommandSourceStack(), true, new FTC(ChatFormatting.GOLD, "Your request to "), new FTC(ChatFormatting.GREEN, tp.getSubject()), new FTC(ChatFormatting.GOLD, " has been denied."));
        }
    }

    public static void sendMessage(CommandSourceStack source, boolean success, TextComponent... styled) {
        if(styled.length > 0) {
            TextComponent comp = styled[0];
            if(styled.length > 1) {
                for(int i = 1; i < styled.length; i++) {
                    comp.append(styled[i]);
                }
            }
            if(success) {
                source.sendSuccess(comp, false);
            } else {
                source.sendFailure(comp);
            }
        }
    }

}
