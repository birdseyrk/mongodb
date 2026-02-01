package com.example.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public class HybridEventRepository {

    private final MongoCollection<HybridEvent> collection;

    public HybridEventRepository(MongoCollection<HybridEvent> collection) {
        this.collection = collection;
        ensureIndexesNoCrash();
    }

    // Avoid crashing on your TTL index conflict
    private void ensureIndexesNoCrash() {
        Set<String> names = new HashSet<>();
        for (Document d : collection.listIndexes()) {
            names.add(d.getString("name"));
        }

        // Don't recreate event_ts_1 (it already exists with TTL in your DB)
        // Only create if missing
        if (!names.contains("event_ts_1")) {
            collection.createIndex(Indexes.ascending("event_ts"), new IndexOptions().name("event_ts_1"));
        }

        // Optional for fast lookup by business id
        if (!names.contains("id_1")) {
            collection.createIndex(Indexes.ascending("id"), new IndexOptions().name("id_1"));
        }
    }

    /** Business id (field name is BSON "id") */
    public Optional<HybridEvent> findOneById(String id) {
        return Optional.ofNullable(collection.find(eq("id", id)).first());
    }

    /** Mongo _id (ObjectId) lookup */
    public Optional<HybridEvent> findOneByMongoObjectId(ObjectId oid) {
        return Optional.ofNullable(collection.find(eq("_id", oid)).first());
    }

    public Optional<HybridEvent> findOneByMongoId(String mongoIdHex) {
        return Optional.ofNullable(collection.find(eq("_id", new ObjectId(mongoIdHex))).first());
    }


    /** Convenience: find by hex string */
    public Optional<HybridEvent> findOneByMongoIdHex(String hex) {
        return findOneByMongoObjectId(new ObjectId(hex));
    }

    /** event_ts lookup (stored as BSON Date; driver will match Instant correctly with POJO codec) */
    public Optional<HybridEvent> findOneByEventTs(Instant ts) {
        return Optional.ofNullable(collection.find(eq("event_ts", ts)).first());
    }

    public List<HybridEvent> findAll() {
        List<HybridEvent> out = new ArrayList<>();
        collection.find().into(out);
        return out;
    }

    public void insertOne(HybridEvent event) {
        collection.insertOne(event);
    }

    public void insertMany(List<HybridEvent> events) {
        if (events == null || events.isEmpty()) return;
        collection.insertMany(events);
    }
}
