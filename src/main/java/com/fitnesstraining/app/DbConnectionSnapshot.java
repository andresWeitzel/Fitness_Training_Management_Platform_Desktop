package com.fitnesstraining.app;

public record DbConnectionSnapshot(Status status, String tooltip) {

    public enum Status {
        UNKNOWN,
        CONNECTED,
        DISCONNECTED
    }

    public static DbConnectionSnapshot unknown() {
        return new DbConnectionSnapshot(Status.UNKNOWN, null);
    }

    public static DbConnectionSnapshot connected(String tooltip) {
        return new DbConnectionSnapshot(Status.CONNECTED, tooltip);
    }

    public static DbConnectionSnapshot disconnected(String tooltip) {
        return new DbConnectionSnapshot(Status.DISCONNECTED, tooltip);
    }
}
