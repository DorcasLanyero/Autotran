package com.cassens.autotran.data;

import android.util.Log;

import com.cassens.autotran.BuildConfig;
import com.cassens.autotran.EventBusIndex;
import com.cassens.autotran.Logs;
import com.cassens.autotran.data.event.Event;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EventBusManager {
    private static final Logger log = LoggerFactory.getLogger(EventBusManager.class.getSimpleName());

    // ---- Public interface
    public enum Queue {
        NETWORK_REQUESTS,
        DRIVER_ACTIONS,
    }

    public interface EventCallback {
        /**
         *
         * @param e The event.
         * @return True, if the handler wants to be removed from consideration
         * for future events.
         */
        boolean onEvent(Event e);
    }

    public static int generateId() {
        return random.nextInt(Integer.MAX_VALUE);
    }

    public void listenForEvents(Queue q, EventCallback c) {
        Logs.plusLogcat(log, Logs.EVENT_BUS, "Registered listener for " + q + ": " + c);

        CallbackListEntry e = new CallbackListEntry();
        e.callback = c;
        e.queue = q;

        synchronized (mCallbackList) {
            mCallbackList.add(e);
        }
    }

    public void unregisterListener(EventCallback c) {
        synchronized (mCallbackList) {
            CallbackListEntry toRemove = null;

            for(CallbackListEntry e : mCallbackList) {
                if(c.equals(e.callback)){
                    Logs.plusLogcat(log, Logs.EVENT_BUS,"Unregistered listener for " + e.queue + ": " + c);
                    toRemove = e;
                    break;
                }
            }

            mCallbackList.remove(toRemove);
        }
    }

    public void publish(Event e) {
        EventBus.getDefault().post(e);
    }

    // ---- Event handling and queues
    private final List<CallbackListEntry> mCallbackList = new ArrayList<>();

    private class CallbackListEntry {
        EventCallback callback;
        Queue queue;
    }

    /**
     * Internal use.
     */
    @Subscribe
    public void handleEvent(Event e) {
        Logs.plusLogcat(log, Logs.EVENT_BUS,"Event: " + e);
        synchronized (mCallbackList) {
            List<CallbackListEntry> toRemove = new ArrayList<>();

            for(CallbackListEntry l : mCallbackList) {
                if(l.queue == e.queue) {
                    if(l.callback.onEvent(e)) {
                        toRemove.add(l);
                    }
                }
            }

            mCallbackList.removeAll(toRemove);
        }
    }

    // ---- Singleton boilerplate
    private static final Random random = new Random();
    private static EventBusManager instance;

    public static EventBusManager getInstance() {
        if(instance == null) {
            instance = new EventBusManager();
        }

        return instance;
    }

    private EventBusManager() {
        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();
        EventBus.getDefault().register(this);
    }
}
