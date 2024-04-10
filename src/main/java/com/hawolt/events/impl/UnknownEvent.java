package com.hawolt.events.impl;

import com.hawolt.events.BaseEvent;
import com.hawolt.events.Event;

public class UnknownEvent extends Event {
    public UnknownEvent(BaseEvent base) {
        super(base);
    }

    @Override
    public String getType() {
        return "UNKNOWN";
    }
}
