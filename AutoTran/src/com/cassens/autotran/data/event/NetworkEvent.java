package com.cassens.autotran.data.event;

import com.cassens.autotran.data.EventBusManager;

public abstract class NetworkEvent extends Event {
    public enum Result {
        UNKNOWN,
        SUCCESS,
        SERVER_ERROR,
        TIMEOUT,
        S3_ERROR,
    }

    public Result result;
    public NetworkEvent(EventBusManager.Queue q, Result r) {
        super(q);
        result = r;
    }

    @Override
    public String toString() {
        return super.toString() + " R:" + result;
    }
}
