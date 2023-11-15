package com.hawolt.user;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record UserMetadata(
        String displayName, String color,
        boolean mod, boolean subscriber, boolean turbo,
        boolean firstMessage, boolean returningChatter,
        long userId, long room,
        List<BadgeInfo> badges
) {
    /* currently untracked values
        user-type=,
        tmi-sent-ts=1700065684151,
        flags=,
        emotes=,
        id=edb6fb2b-feb6-4983-950f-113b5d9b11b4,
     */
    public UserMetadata(Map<String, String> map) {
        this(
                map.get("display-name"),
                map.get("color"),
                1 == Integer.parseInt(map.get("mod")),
                1 == Integer.parseInt(map.get("subscriber")),
                1 == Integer.parseInt(map.get("turbo")),
                1 == Integer.parseInt(map.get("first-msg")),
                1 == Integer.parseInt(map.get("returning-chatter")),
                Long.parseLong(map.get("user-id")),
                Long.parseLong(map.get("room-id")),
                Arrays.stream(map.get("badges").split(","))
                        .filter(o -> o.length() == 2)
                        .map(BadgeInfo::new)
                        .collect(Collectors.toList())
        );
    }

    public boolean broadcaster() {
        return badges.stream().anyMatch(
                badgeInfo -> badgeInfo.name().equals("broadcaster")
        );
    }
}

