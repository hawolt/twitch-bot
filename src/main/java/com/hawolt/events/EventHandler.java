package com.hawolt.events;

public interface EventHandler<T extends Event> {
    void onEvent(T t);
}
