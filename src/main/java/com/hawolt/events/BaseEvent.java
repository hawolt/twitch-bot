package com.hawolt.events;

import com.hawolt.Bot;

public record BaseEvent(Bot bot, String[] data) {

}