package com.hawolt.events.impl;

import com.hawolt.events.Event;

public class UnknownEvent extends Event {
    public UnknownEvent(String[] data) {
        super(data);
    }

    @Override
    public String getType() {
        return "UNKNOWN";
    }
}
