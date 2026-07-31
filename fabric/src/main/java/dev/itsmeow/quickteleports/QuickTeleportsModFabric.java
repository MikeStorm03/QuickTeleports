package dev.itsmeow.quickteleports;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.ClickEvent;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.msg.quickteleports.ForkedAtCommon;

import io.github.fablabsmc.fablabs.api.fiber.v1.builder.ConfigTreeBuilder;
import io.github.fablabsmc.fablabs.api.fiber.v1.exception.ValueDeserializationException;

import java.io.File;
import java.nio.file.Files;

import io.github.fablabsmc.fablabs.api.fiber.v1.schema.type.derived.ConfigTypes;
import io.github.fablabsmc.fablabs.api.fiber.v1.schema.type.derived.NumberConfigType;
import io.github.fablabsmc.fablabs.api.fiber.v1.serialization.FiberSerialization;
import io.github.fablabsmc.fablabs.api.fiber.v1.serialization.JanksonValueSerializer;
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigBranch;
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigTree;
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.PropertyMirror;

public class QuickTeleportsModFabric implements ModInitializer {

    private static final NumberConfigType<Integer> TYPE = ConfigTypes.INTEGER.withValidRange(Constants.CONFIG_FIELD_MIN, Constants.CONFIG_FIELD_MAX, 1);
    public static final PropertyMirror<Integer> teleportRequestTimeout = PropertyMirror.create(TYPE);
    protected static final JanksonValueSerializer JANKSON_VALUE_SERIALIZER = new JanksonValueSerializer(false);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            QuickTeleportsMod.registerCommands(dispatcher);
            ForkedAtCommon.registerCommands(dispatcher);
        });
        ServerTickEvents.START_SERVER_TICK.register(QuickTeleportsMod::serverTick);
        ServerLifecycleEvents.SERVER_STARTING.register(state -> {
            ConfigTreeBuilder builder = ConfigTree.builder().withName(Constants.MOD_ID).beginValue(Constants.CONFIG_FIELD_NAME, TYPE, Constants.CONFIG_FIELD_VALUE).withComment(Constants.CONFIG_FIELD_COMMENT).finishValue(teleportRequestTimeout::mirror);
            ConfigBranch branch = builder.build();
            File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), Constants.MOD_ID + ".json5");
            boolean recreate = false;
            while (true) {
                try {
                    if (!configFile.exists() || recreate) {
                        FiberSerialization.serialize(branch, Files.newOutputStream(configFile.toPath()), JANKSON_VALUE_SERIALIZER);
                        break;
                    } else {
                        try {
                            FiberSerialization.deserialize(branch, Files.newInputStream(configFile.toPath()), JANKSON_VALUE_SERIALIZER);
                            FiberSerialization.serialize(branch, Files.newOutputStream(configFile.toPath()), JANKSON_VALUE_SERIALIZER);
                            break;
                        } catch (ValueDeserializationException e) {
                            String fileName = (Constants.MOD_ID + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss")) + ".json5");
                            configFile.renameTo(new File(configFile.getParent(), fileName));
                            recreate = true;
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
    }
}
