package com.hawolt;

import com.hawolt.data.Capability;
import com.hawolt.data.Constant;
import com.hawolt.data.Environment;
import com.hawolt.events.Event;
import com.hawolt.events.EventHandler;
import com.hawolt.events.impl.MessageEvent;
import com.hawolt.events.impl.UnknownEvent;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Bot implements Handler {

    private static Map<String, Function<String[], Event>> map = new HashMap<>() {{
        put("PRIVMSG", MessageEvent::new);
    }};
    private static Map<Class<? extends Event>, List<EventHandler<?>>> handlers = new HashMap<>();
    private final Environment environment;
    private final Connection connection;
    private final Socket socket;

    private Bot(Environment environment) {
        this.environment = environment;
        this.socket = getSocket();
        this.connection = Connection.connect(this);
    }

    public void register(Class<? extends Event> event, EventHandler<?> handler) {
        if (!handlers.containsKey(event.getClass())) {
            handlers.put(event, new LinkedList<>());
        }
        handlers.get(event).add(handler);
    }

    // create a Socket connection to the switch server
    private Socket getSocket() {
        try {
            return new Socket(
                    Constant.TWITCH_HOSTNAME,
                    Constant.TWITCH_PORT
            );
        } catch (IOException e) {
            System.err.println("Failed to create Socket");
            System.exit(1);
        }
        return null;
    }

    public static Bot connect(
            Environment environment,
            Capability... capabilities
    ) throws IOException {
        Bot bot = new Bot(environment);
        bot.requestCapabilities(capabilities);
        bot.connection.sendRAW(bot.getPassLine());
        bot.connection.sendRAW(bot.getNickLine());
        return bot;
    }

    @Override
    public void onInput(String line) {
        System.out.println("< " + line);
        if (line.startsWith("PING")) {
            String message = line.split(" ", 2)[1];
            try {
                connection.sendRAW(
                        String.join(
                                " ",
                                "PONG",
                                message
                        )
                );
            } catch (IOException e) {
                System.err.println("Failed to send PONG");
            }
        } else {
            boolean comesWithTags = line.startsWith("@");
            String[] data = line.split(" ", comesWithTags ? 5 : 4);
            String type = data[comesWithTags ? 2 : 1];
            Event event = map.getOrDefault(type, UnknownEvent::new).apply(data);
            Optional.ofNullable(handlers.get(event.getClass())).ifPresent(list -> {
                list.forEach(handler -> handler.onEvent(
                        cast(event)
                ));
            });
        }
    }

    @SuppressWarnings("all")
    private <T> T cast(Object o) {
        return (T) o;
    }

    public String getNickLine() {
        // fetch username from environment and format data
        String username = environment.get("USERNAME");
        return String.join(
                " ",
                "NICK",
                username
        );
    }

    public String getPassLine() {
        // fetch token from environment and format data
        String token = environment.get("ACCESS_TOKEN");
        String oauth = String.join(
                ":",
                "oauth",
                token
        );
        return String.join(
                " ",
                "PASS",
                oauth
        );
    }

    public Environment getEnvironment() {
        return environment;
    }

    public Socket getSocketInstance() {
        return socket;
    }

    // request one or multiple capabilities to get more data
    private void requestCapabilities(Capability... capabilities) throws IOException {
        if (capabilities.length == 0) return;
        String capability = Arrays.stream(capabilities)
                .map(Capability::toString)
                .collect(Collectors.joining(" "));
        connection.sendRAW("CAP REQ :" + capability);
    }

    // join one or multiple twitch channels
    public void join(String... channels) throws IOException {
        connection.sendRAW(
                String.format(
                        "JOIN #%s",
                        String.join(",", channels)
                )
        );
    }

    // join a twitch channel
    public void join(String channel) throws IOException {
        connection.sendRAW(
                String.format(
                        "JOIN #%s",
                        channel
                )
        );
    }
}
