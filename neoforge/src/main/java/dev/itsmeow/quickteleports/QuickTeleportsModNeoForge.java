package dev.itsmeow.quickteleports;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@Mod(Constants.MOD_ID)
@EventBusSubscriber(modid = Constants.MOD_ID)
public class QuickTeleportsModNeoForge {

    public static ServerConfig SERVER_CONFIG = null;
    private static ModConfigSpec SERVER_CONFIG_SPEC = null;

    public QuickTeleportsModNeoForge() {
        // ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (s, b) -> true));
        // FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadComplete);
    }

    private void loadComplete(final FMLLoadCompleteEvent event) {
        final Pair<ServerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_CONFIG_SPEC = specPair.getRight();
        SERVER_CONFIG = specPair.getLeft();
        // ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG_SPEC);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        QuickTeleportsMod.serverTick(ServerLifecycleHooks.getCurrentServer());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        QuickTeleportsMod.registerCommands(event.getDispatcher());
    }

    public static class ServerConfig {
        public ModConfigSpec.Builder builder;
        public final ModConfigSpec.IntValue teleportRequestTimeout;

        ServerConfig(ModConfigSpec.Builder builder) {
            this.builder = builder;
            this.teleportRequestTimeout = builder.comment(Constants.CONFIG_FIELD_COMMENT + " Place a copy of this config in the defaultconfigs/ folder in the main server/.minecraft directory (or make the folder if it's not there) to copy this to new worlds.")
            .defineInRange(Constants.CONFIG_FIELD_NAME, Constants.CONFIG_FIELD_VALUE, Constants.CONFIG_FIELD_MIN, Constants.CONFIG_FIELD_MAX);
            builder.build();
        }
    }
}