package com.cassens.autotran.data.event;

import com.cassens.autotran.data.EventBusManager;

public class S3UploadEvent extends NetworkEvent {
    public final int id;

    public S3UploadEvent(int id, Result r) {
        super(EventBusManager.Queue.NETWORK_REQUESTS, r);

        this.id = id;
    }

    @Override
    public String toString() {
        return super.toString() + " id:" + id;
    }
}
