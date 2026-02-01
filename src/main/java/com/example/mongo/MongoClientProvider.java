package com.example.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.Document;

import static org.bson.codecs.configuration.CodecRegistries.*;

public class MongoClientProvider implements AutoCloseable {

    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public MongoClientProvider(Config cfg) {
        String uri = "mongodb://" + cfg.mongoUser + ":" + cfg.mongoPassword + "@"
                + cfg.mongoHost + ":" + cfg.mongoPort + "/?authSource=" + cfg.mongoAuthDb; //rkb

        System.out.println("**** mongo connection string  " + uri); //rkb

        CodecRegistry pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .codecRegistry(pojoCodecRegistry)
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(cfg.mongoDb);
    }

    public MongoCollection<HybridEvent> getCollection(Config cfg) {
        return database.getCollection(cfg.mongoCollection, HybridEvent.class);
    }
    
    public MongoCollection<Document> getRawCollection(String dbName, String collectionName) {
        return mongoClient.getDatabase(dbName).getCollection(collectionName);
    }

    public com.mongodb.client.MongoClient getClient() {
        return mongoClient;
    }

    public MongoDatabase getDatabase(String dbName) {
        return getClient().getDatabase(dbName);
    }

    @Override
    public void close() {
        mongoClient.close();
    }
}
