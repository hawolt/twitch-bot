package com.hawolt.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class Environment extends HashMap<String, String> {
    public Environment() {
        // get the current running directory
        String dir = System.getProperty("user.dir");
        // obtain path to hopefully present .env file
        Path path = Paths.get(".env");
        try {
            // read every line within the file
            // put the values into our map
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String[] pair = line.split("=", 2);
                if (pair.length != 2) continue;
                put(pair[0], pair[1]);
            }
        } catch (IOException e) {
            System.err.println("Failed to read file '.env' in '" + dir + "'.");
        }
    }

    // method to check if all values we need are configured
    public boolean isProperlyConfigured() {
        return containsKey("ACCESS_TOKEN") && containsKey("USERNAME");
    }
}
