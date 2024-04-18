package com.hawolt;

import com.hawolt.logger.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Connection implements Runnable {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler;
    private boolean interrupted;
    private final String token;
    private Future<?> future;
    private final Bot bot;
    private Socket socket;

    private Connection(Bot bot) {
        this.token = bot.getEnvironment().get("ACCESS_TOKEN");
        this.socket = bot.getSocketInstance();
        this.handler = bot;
        this.bot = bot;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    // start the connection and make sure it is not already running
    public static Connection connect(Bot bot) {
        Connection connection = new Connection(bot);
        connection.connect();
        return connection;
    }

    private void connect() {
        if (future != null && !future.isDone()) {
            if (socket.isClosed()) {
                bot.setSocket(bot.getSocket());
                socket = bot.getSocketInstance();
            } else {
                Logger.error("already connected");
            }
        } else {
            future = executorService.submit(this);
        }
    }

    public void close() throws IOException {
        if (socket.isClosed()) return;
        this.socket.close();
    }

    public void reconnect() {
        try {
            this.close();
            this.connect();
            this.bot.login();
        } catch (IOException e) {
            Logger.error(e.getMessage());
        }
    }

    public void ready() {
        try {
            this.bot.ready();
        } catch (IOException e) {
            Logger.error(e.getMessage());
        }
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

        Logger.debug("[ws-out] {}", message);
        outputStream.flush();
    }

    public void interrupt() {
        this.interrupted = true;
    }

    @Override
    public void run() {
        do {
            try (InputStream inputStream = socket.getInputStream()) {
                Traffic traffic = new Traffic();
                while (socket.isConnected()) {
                    byte b = (byte) inputStream.read();
                    traffic.add(b);
                    if (b == '\n') {
                        String in = new String(
                                traffic.get(),
                                StandardCharsets.UTF_8
                        );
                        handler.onInput(in.trim());
                    }
                }
            } catch (IOException e) {
                Logger.error(e.getMessage());
            }
            if (interrupted) return;
            this.reconnect();
        } while (!interrupted);
    }
}
