package com.msg.quickteleports.platform;

import com.msg.quickteleports.platform.services.IPlatformHelper;

import dev.itsmeow.quickteleports.QuickTeleportsModNeoForge;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgePlatformHelper implements IPlatformHelper{

    @Override
    public String getPlatformName() {

        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }

    @Override
    public int getTeleportTimeout() {
        return QuickTeleportsModNeoForge.SERVER_CONFIG.teleportRequestTimeout.get();
    }
}