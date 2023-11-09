package com.hawolt;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Bot implements Handler {
    private final Environment environment;
    private final Connection connection;
    private final Socket socket;

    private Bot(Environment environment) {
        this.environment = environment;
        this.socket = getSocket();
        this.connection = Connection.connect(this);
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
        }
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
