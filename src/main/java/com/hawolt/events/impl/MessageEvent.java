package com.hawolt.events.impl;

import com.hawolt.events.Event;
import com.hawolt.user.UserMetadata;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MessageEvent extends Event {
    private final String message, channel, type, serverName, nickName, host;
    private UserMetadata userMetadata;

    public MessageEvent(String[] data) {
        super(data);
        this.message = data[data.length - 1].substring(1);
        this.channel = data[data.length - 2];
        this.type = data[data.length - 3];
        String[] prefix = data[data.length - 4].split("@");
        this.host = prefix[1];
        String[] names = prefix[0].split("!");
        this.serverName = names[0].substring(1);
        this.nickName = names[1];
        if (!data[0].startsWith("@")) return;
        Map<String, String> tags = new HashMap<>();
        String[] metadata = data[0].substring(1).split(";");
        for (String pair : metadata) {
            String[] values = pair.split("=");
            tags.put(values[0], values.length == 2 ? values[1] : "");
        }
        this.userMetadata = new UserMetadata(tags);
    }

    public Optional<UserMetadata> getUserMetadata() {
        return Optional.ofNullable(userMetadata);
    }

    public String getMessage() {
        return message;
    }

    public String getChannel() {
        return channel;
    }

    public String getServerName() {
        return serverName;
    }

    public String getNickName() {
        return nickName;
    }

    public String getHost() {
        return host;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "MessageEvent{" +
                "message='" + message + '\'' +
                ", channel='" + channel + '\'' +
                ", type='" + type + '\'' +
                ", serverName='" + serverName + '\'' +
                ", nickName='" + nickName + '\'' +
                ", host='" + host + '\'' +
                ", userMetadata=" + userMetadata +
                ", timestamp=" + timestamp +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
