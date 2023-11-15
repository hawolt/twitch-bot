package com.hawolt.events;

public abstract class Event {
    protected final long timestamp = System.currentTimeMillis();
    protected final String[] data;

    public Event(String[] data) {
        this.data = data;
    }

    public abstract String getType();

    public long getTimestamp() {
        return timestamp;
    }
}
