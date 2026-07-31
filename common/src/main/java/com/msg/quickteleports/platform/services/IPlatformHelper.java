package com.msg.quickteleports.platform.services;

public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    int getTeleportTimeout();

    default String getEnvironmentName() {

        return isDevelopmentEnvironment() ? "development" : "production";
    }

}