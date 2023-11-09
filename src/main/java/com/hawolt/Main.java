package com.hawolt;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        // load our .env file to access its values
        // without leaking them on stream
        Environment environment = new Environment();
        // check if the needed values are present
        if (!environment.isProperlyConfigured()) {
            System.err.println(
                    """
                            Environment is not properly configured
                            Please add the following entries:
                            ACCESS_TOKEN, USERNAME
                            """
            );
            System.exit(1);
        }
        ExecutorService executorService =
                Executors.newSingleThreadExecutor();
        try {
            // fetch token and username from environment
            String token = environment.get("ACCESS_TOKEN");
            String username = environment.get("USERNAME");
            // open a connection to the switch server
            Socket socket = new Socket(
                    Constant.TWITCH_HOSTNAME,
                    Constant.TWITCH_PORT
            );
            Connection connection = new Connection(socket);
            // prepare string to initialize connection
            String oauth = String.join(
                    ":",
                    "oauth",
                    token
            );
            String pass = String.join(
                    " ",
                    "PASS",
                    oauth
            );
            // send password string to twitch
            connection.sendRAW(pass);
            String nick = String.join(
                    " ",
                    "NICK",
                    username
            );
            // send nickname string to twitch
            connection.sendRAW(nick);
            // start reading data from twitch server
            // this happens in the background
            executorService.execute(connection);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
