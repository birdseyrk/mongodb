package com.example.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * MongoClientProviderTest
 *
 * - Creates ONE MongoClient for the app lifetime
 * - Provides a typed MongoCollection<HybridEvent> with POJO codec enabled
 * - Supports:
 *   A) standard config (host/port/user/password/authDb)
 *   B) direct connectionString (Testcontainers, etc.)
 *
 * Important:
 * - MongoClient is thread-safe and should be reused.
 * - Call close() when the app shuts down.
 */
public class MongoClientProviderTest implements AutoCloseable {

    private final MongoClient client;
    private final CodecRegistry pojoCodecRegistry;

    /**
     * Constructor for production usage using a MongoDB URI.
     * Example URI:
     *   mongodb://user:pass@host:27017/?authSource=admin
     */
    public MongoClientProviderTest(String mongoUri) {
        this.pojoCodecRegistry = buildPojoCodecRegistry();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .codecRegistry(pojoCodecRegistry)
                .build();

        this.client = MongoClients.create(settings);
    }

    /**
     * Constructor for your Config-based app usage.
     * Requires Config fields (recommended):
     *  - mongoHost
     *  - mongoPort
     *  - mongoUser
     *  - mongoPassword
     *  - mongoAuthDb   (optional; default "admin")
     */
    public MongoClientProviderTest(Config cfg) {
        this(buildUriFromConfig(cfg));
    }

    /**
     * Get a typed collection with POJO codec enabled.
     */
    public MongoCollection<HybridEvent> getCollection(String dbName, String collectionName) {
        return client.getDatabase(dbName)
                .withCodecRegistry(pojoCodecRegistry)
                .getCollection(collectionName, HybridEvent.class);
    }

    /**
     * Convenience for your app: uses cfg.mongoDb and cfg.mongoCollection
     */
    public MongoCollection<HybridEvent> getCollection(Config cfg) {
        return getCollection(cfg.mongoDb, cfg.mongoCollection);
    }

    public MongoClient getClient() {
        return client;
    }

    @Override
    public void close() {
        client.close();
    }

    // ------------------ helpers ------------------

    private static CodecRegistry buildPojoCodecRegistry() {
        return fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );
    }

    private static String buildUriFromConfig(Config cfg) {
        // If you already keep a full URI in env/config, you can add:
        // if (cfg.mongoUri != null && !cfg.mongoUri.isBlank()) return cfg.mongoUri;

        String user = cfg.mongoUser;
        String pass = cfg.mongoPassword;

        // auth DB is where the user is defined (often "admin")
        String authDb = (cfg.mongoAuthDb == null || cfg.mongoAuthDb.isBlank()) ? "admin" : cfg.mongoAuthDb;

        // Note: if your password can contain special chars, you should URL-encode it.
        // For lab passwords like admin/admin this is fine.
        return "mongodb://" + user + ":" + pass + "@"
                + cfg.mongoHost + ":" + cfg.mongoPort
                + "/?authSource=" + authDb;
    }
}

