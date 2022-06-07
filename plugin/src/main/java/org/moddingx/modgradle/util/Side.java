package org.moddingx.modgradle.util;

public enum Side {
    
    CLIENT("client", true, false),
    SERVER("server", false, true),
    COMMON("common", true, true);

    Side(String id, boolean client, boolean server) {
        this.id = id;
        this.client = client;
        this.server = server;
    }
    
    public final String id;
    public final boolean client;
    public final boolean server;
    
    public static Side byId(String id) {
        return switch (id) {
            case "client" -> CLIENT;
            case "server" -> SERVER;
            case "common" -> COMMON;
            default -> throw new IllegalStateException("Unknown side: " + id);
        };
    }
}
