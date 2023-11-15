package com.hawolt.user;

public record BadgeInfo(String name, int value) {
    public BadgeInfo(String plain) {
        this(
                plain.split("/")[0],
                Integer.parseInt(plain.split("/")[1])
        );
    }
}
