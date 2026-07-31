package com.msg.quickteleports.memory;

import dev.itsmeow.quickteleports.Constants;
import dev.itsmeow.quickteleports.util.Teleport;

public class TeleportRequest {
    private final Teleport teleport;
    private int timeoutTicks;

    public TeleportRequest(Teleport teleport, int timeoutTicks) {
        this.teleport = teleport;
        this.timeoutTicks = timeoutTicks;
    }

    public Teleport getTeleport() {
        return this.teleport;
    }

    public int getTimeoutTicks() {
        return timeoutTicks;
    }

    public void decrementTimeout() {
        timeoutTicks = Math.max(0, timeoutTicks - 1);
    }

    public boolean isExpired() {
        return timeoutTicks <= 0;
    }
    
}
