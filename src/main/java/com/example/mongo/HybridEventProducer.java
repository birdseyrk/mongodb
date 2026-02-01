package com.example.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.Instant;
import java.util.Map;
import java.util.Random;

import static com.mongodb.client.model.Filters.eq;

public class HybridEventProducer {

    private static final String COUNTER_ID = "hybrid_events_id_seq";

    private final MongoCollection<HybridEvent> events;
    private final MongoCollection<Document> counters;
    private final Random rnd = new Random();

    public HybridEventProducer(MongoCollection<HybridEvent> eventsCollection,
                               MongoCollection<Document> countersCollection) {
        this.events = eventsCollection;
        this.counters = countersCollection;
    }

    /** Atomic sequence generator (safe even with multiple producers). */
    public long nextSequence() {
        Document updated = counters.findOneAndUpdate(
                eq("_id", COUNTER_ID),
                Updates.combine(
                        Updates.inc("seq", 1),
                        Updates.setOnInsert("_id", COUNTER_ID)
                ),
                new FindOneAndUpdateOptions()
                        .upsert(true)
                        .returnDocument(ReturnDocument.AFTER)
        );

        Object seqObj = updated.get("seq");
        if (seqObj instanceof Number n) return n.longValue();
        throw new IllegalStateException("Counter seq is not numeric: " + seqObj);
    }

    public HybridEvent buildNextEvent(long seq) {
        HybridEvent e = new HybridEvent();

        e.setEventTs(Instant.now());
        e.setId("id_" + seq);

        Map<String, Object> payload = Map.of(
                "temperature", rnd.nextInt(100),
                "status", (seq % 2 == 0) ? "OK" : "WARN",
                "meta", Map.of(
                        "sequence", seq,
                        "source", "ansible"
                )
        );
        e.setPayload(payload);

        return e;
    }

    public HybridEvent insertNext() {
        long seq = nextSequence();
        HybridEvent e = buildNextEvent(seq);
        events.insertOne(e);
        return e;
    }

    public void runForever(long throttleSeconds) {
        while (true) {
            insertNext();
            sleepSeconds(throttleSeconds);
        }
    }

    public void runNTimes(int n, long throttleSeconds) {
        for (int i = 0; i < n; i++) {
            insertNext();
            sleepSeconds(throttleSeconds);
        }
    }

    private static void sleepSeconds(long seconds) {
        if (seconds <= 0) return;
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Producer interrupted", ie);
        }
    }
}
