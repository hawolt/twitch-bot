package com.hawolt;

import com.hawolt.data.Capability;
import com.hawolt.data.Constant;
import com.hawolt.data.Environment;
import com.hawolt.events.BaseEvent;
import com.hawolt.events.Event;
import com.hawolt.events.EventHandler;
import com.hawolt.events.impl.MessageEvent;
import com.hawolt.events.impl.UnknownEvent;
import com.hawolt.logger.Logger;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Bot implements Handler {

    private static final Map<Class<? extends Event>, List<EventHandler<?>>> handlers = new HashMap<>();
    private static final ExecutorService service = Executors.newCachedThreadPool();
    private static final Map<String, Function<BaseEvent, Event>> map = new HashMap<>() {{
        put("PRIVMSG", MessageEvent::new);
    }};
    private final Set<String> channels = new HashSet<>();
    private final Supplier<String[]> supplier;
    private final Capability[] capabilities;
    private final Environment environment;
    private final Connection connection;
    private Socket socket;

    private Bot(Environment environment, Supplier<String[]> supplier, Capability[] capabilities) {
        this.supplier = supplier;
        this.socket = getSocket();
        this.environment = environment;
        this.capabilities = capabilities;
        this.connection = Connection.connect(this);
    }

    public void shutdown() {
        this.connection.interrupt();
        this.connection.getExecutorService().shutdown();
        try {
            this.socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Bot.service.shutdown();
        }
    }

    public boolean isMember(String channel) {
        return this.channels.contains(channel);
    }

    public void register(Class<? extends Event> event, EventHandler<?> handler) {
        if (!handlers.containsKey(event)) {
            handlers.put(event, new LinkedList<>());
        }
        handlers.get(event).add(handler);
    }

    // create a Socket connection to the switch server
    public Socket getSocket() {
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

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public static Bot connect(
            Environment environment,
            Capability... capabilities
    ) throws IOException {
        return connect(environment, () -> new String[0], capabilities);
    }

    public static Bot connect(
            Environment environment,
            Supplier<String[]> supplier,
            Capability... capabilities
    ) throws IOException {
        Bot bot = new Bot(environment, supplier, capabilities);
        bot.login();
        return bot;
    }

    public void login() throws IOException {
        requestCapabilities(capabilities);
        connection.sendRAW(getPassLine());
        connection.sendRAW(getNickLine());
        for (String channel : supplier.get()) {
            join(channel);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void onInput(String line) {
        Logger.debug("[ws-in] {}", line);
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
            if (data[1].equals("RECONNECT")) {
                connection.reconnect();
            } else {
                String type = data[comesWithTags ? 2 : 1];
                BaseEvent base = new BaseEvent(this, data);
                Event event = map.getOrDefault(type, UnknownEvent::new).apply(base);
                Optional.ofNullable(handlers.get(event.getClass())).ifPresent(list -> {
                    list.forEach(handler -> {
                        service.execute(() -> {
                            handler.onEvent(cast(event));
                        });
                    });
                });
            }
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

    private final Object lock = new Object();

    // join one or multiple twitch channels
    public void join(String... channels) throws IOException {
        connection.sendRAW(
                String.format(
                        "JOIN #%s",
                        String.join(",", channels)
                )
        );
        synchronized (lock) {
            this.channels.addAll(Arrays.asList(channels));
        }
    }

    // join a twitch channel
    public void join(String channel) throws IOException {
        connection.sendRAW(
                String.format(
                        "JOIN #%s",
                        channel
                )
        );
        synchronized (lock) {
            this.channels.add(channel);
        }
    }

    // part a twitch channel
    public void part(String... channels) throws IOException {
        connection.sendRAW(
                String.format(
                        "PART #%s",
                        String.join(",", channels)
                )
        );
        for (String channel : channels) {
            synchronized (lock) {
                this.channels.remove(channel);
            }
        }
    }

    // part a twitch channel
    public void part(String channel) throws IOException {
        connection.sendRAW(
                String.format(
                        "PART #%s",
                        channel
                )
        );
        synchronized (lock) {
            this.channels.remove(channel);
        }
    }

    // send a message to a twitch channel
    public void send(String channel, String message) throws IOException {
        connection.sendRAW(
                String.format(
                        "PRIVMSG #%s :%s\n",
                        channel,
                        message
                )
        );
    }
}
