package com.hawolt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Connection implements Runnable {
    private final Socket socket;

    public Connection(Socket socket) {
        this.socket = socket;
    }

    // method to send data to twitch in the correct format
    // data needs to end with a newline character
    public void sendRAW(Object o) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write((o + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    @Override
    public void run() {
        try (InputStream inputStream = socket.getInputStream()) {
            // loop while we are connected to twitch
            while (socket.isConnected()) {
                // read the first byte, required for available
                // to know how much data there is
                byte b = (byte) inputStream.read();
                int available = inputStream.available();
                // construct byte array containing all data
                byte[] bytes = new byte[available];
                inputStream.read(bytes);
                byte[] data = new byte[bytes.length + 1];
                data[0] = b;
                System.arraycopy(
                        bytes,
                        0,
                        data,
                        1,
                        bytes.length
                );
                // handle data received, line by line
                String in = new String(data, StandardCharsets.UTF_8);
                String[] lines = in.split("\n");
                for (String line : lines) {
                    System.out.format("> %s\n", line.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
