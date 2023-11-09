package com.hawolt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Connection implements Runnable {
    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();
    private final Handler handler;
    private final Socket socket;
    private final String token;
    private final Bot bot;

    private Future<?> future;

    private Connection(Bot bot) {
        this.token = bot.getEnvironment().get("ACCESS_TOKEN");
        this.socket = bot.getSocketInstance();
        this.handler = bot;
        this.bot = bot;
    }

    // start the connection and make sure it is not already running
    public static Connection connect(Bot bot) {
        Connection connection = new Connection(bot);
        if (connection.future != null && !connection.future.isDone()) {
            System.err.println("already connected");
        } else {
            connection.future =
                    connection.executorService.submit(connection);
        }
        return connection;
    }

    // method to send data to twitch in the correct format
    // data needs to end with a newline character
    public void sendRAW(Object o) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(
                (o + "\r\n").getBytes(StandardCharsets.UTF_8)
        );
        // print what we send, but make sure to not print
        // any private information to the console
        String message = o.toString().replace(
                token,
                "${REDACTED}"
        );
        System.out.println("> " + message);
        outputStream.flush();
    }

    @Override
    public void run() {
        try (InputStream inputStream = socket.getInputStream()) {
            // loop while we are connected to twitch
            Traffic traffic = new Traffic();
            while (socket.isConnected()) {
                byte b = (byte) inputStream.read();
                traffic.add(b);
                // handle data received, when linebreak occurs
                if (b == '\n') {
                    String in = new String(
                            traffic.get(),
                            StandardCharsets.UTF_8
                    );
                    handler.onInput(in.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
