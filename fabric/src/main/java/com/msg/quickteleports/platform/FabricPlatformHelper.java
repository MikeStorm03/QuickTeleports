package com.msg.quickteleports.platform;

import com.msg.quickteleports.platform.services.IPlatformHelper;

import dev.itsmeow.quickteleports.QuickTeleportsModFabric;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public int getTeleportTimeout() {
        return QuickTeleportsModFabric.teleportRequestTimeout.getValue();
    }
}
