package com.cassens.autotran.data.event;

import com.cassens.autotran.data.EventBusManager;

public class DriverActionEvent extends NetworkEvent {
    public enum Type {
        UPLOAD_LOGS,
        UPLOAD_IMAGE,
        UNKNOWN
    }

    public final Type type;
    public final int id;

    public DriverActionEvent(Type t, int id) {
        super(EventBusManager.Queue.DRIVER_ACTIONS, Result.UNKNOWN);
        type = t;
        this.id = id;
    }

    @Override
    public String toString() {
        return super.toString() + " id:" +id  + " T:" + type;
    }
}
