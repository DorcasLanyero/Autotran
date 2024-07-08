package com.cassens.autotran.data.event;

import com.cassens.autotran.data.EventBusManager;

public abstract class Event {
    public final EventBusManager.Queue queue;

    public Event(EventBusManager.Queue q) {
        queue = q;
    }

    @Override
    public String toString() {
        return "N:" + getClass().getSimpleName() + " Q:" + queue;
    }
}
