package com.msg.quickteleports.memory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    private static final Map<UUID, CompoundTag> PLAYER_TAGS = new ConcurrentHashMap<>();


    public static CompoundTag getTag(ServerPlayer player) {
        return PLAYER_TAGS.computeIfAbsent(player.getUUID(), k -> new CompoundTag());
    }

    public static void setPreviousPosition(ServerPlayer player, PreviousPosition pos) {
        getTag(player).put("previousPosition", pos.toTag());
    }

    public static PreviousPosition getPreviousPosition(ServerPlayer player) {
        CompoundTag tag = getTag(player).getCompound("previousPosition").get();
        return tag.isEmpty() ? null : PreviousPosition.fromTag(tag);
    }

    // public static void load(ServerPlayer player) {
    //     try {s
    //         File file = new File(DATA_FOLDER, player.getUUID() + ".nbt");
    //         if (file.exists()) {
    //             CompoundTag tag = NbtIo.read(file);
    //             PLAYER_TAGS.put(player.getUUID(), tag);
    //         } else {
    //             PLAYER_TAGS.put(player.getUUID(), new CompoundTag());
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         PLAYER_TAGS.put(player.getUUID(), new CompoundTag());
    //     }
    // }

    // public static void save(ServerPlayer player) {
    //     try {
    //         CompoundTag tag = getTag(player);
    //         File file = new File(DATA_FOLDER, player.getUUID() + ".nbt");
    //         NbtIo.write(tag, file);
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }

    // public static void remove(ServerPlayer player) {
    //     PLAYER_TAGS.remove(player.getUUID());
    // }
}
