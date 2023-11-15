package com.hawolt;

import com.hawolt.data.Capability;
import com.hawolt.data.Environment;
import com.hawolt.events.Event;
import com.hawolt.events.EventHandler;
import com.hawolt.events.impl.MessageEvent;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // load our .env file to access its values
        // without leaking them on stream
        Environment environment = new Environment();
        // check if the needed values are present
        if (!environment.isProperlyConfigured()) {
            System.err.println("""
                    Environment is not properly configured
                    Please add the following entries:
                    ACCESS_TOKEN, USERNAME
                    """);
            System.exit(1);
        }
        try {
            Bot bot = Bot.connect(environment, Capability.values());

            bot.register(MessageEvent.class, new EventHandler<MessageEvent>() {
                @Override
                public void onEvent(MessageEvent event) {
                    System.err.println(event.getMessage());
                }
            });

            bot.join("hawolt");
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
        }
    }
}
