package com.example.mongo;

public class Config {
    public final String mongoHost       = env("MONGO_HOST", "creede03");
    public final int mongoPort          = Integer.parseInt(env("MONGO_PORT", "27017"));
    public final String mongoDb         = env("MONGO_DB", "hybriddb2"); // user is authenticated in the hybriddb2 database
    public final String mongoAuthDb     = env("MONGO_AUTH_DB", "admin");  //user is authenticated inthe admin database.
    public final String mongoUser       = env("MONGO_USER", "mongoUser1");
    public final String mongoPassword   = required("MONGO_PASSWORD");
    public final String mongoCollection = env("MONGO_COLLECTION", "hybrid_events");

    public final int httpPort = Integer.parseInt(env("APP_PORT", "8080"));
    // Optional: bind address if you want; leaving default behavior is fine
    public final String bindHost = env("APP_BIND_HOST", "0.0.0.0");

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static String required(String k) {
        String v = System.getenv(k);
        if (v == null || v.isBlank()) throw new IllegalStateException(k + " is not set");
        return v;
    }
}
