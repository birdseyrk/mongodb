package com.example.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HybridEventRepositoryTest {

    @Container
    private static final MongoDBContainer mongo = new MongoDBContainer("mongo:8");

    private MongoClientProviderTest provider;
    private MongoCollection<HybridEvent> typedCollection;
    private MongoCollection<Document> rawCollection;
    private HybridEventRepository repo;

    private static final String DB_NAME = "hybriddb2_test";
    private static final String COLLECTION_NAME = "hybrid_events";

    @BeforeEach
    void setUp() {
        assumeTrue(mongo.isRunning(), "MongoDB Testcontainer is not running (Docker not available?)");

        provider = new MongoClientProviderTest(mongo.getConnectionString());

        // typed collection (POJO)
        typedCollection = provider.getCollection(DB_NAME, COLLECTION_NAME);

        // raw collection (BSON Document) - bypasses POJO mapping
        MongoDatabase db = provider.getClient().getDatabase(DB_NAME);
        rawCollection = db.getCollection(COLLECTION_NAME);

        // clean slate
        rawCollection.drop();

        repo = new HybridEventRepository(typedCollection);
    }

    @AfterEach
    void tearDown() {
        if (provider != null) provider.close();
    }

    private static HybridEvent sampleEvent(int i) {
        HybridEvent e = new HybridEvent();
        e.setEventTs(Instant.parse("2026-01-30T12:34:56Z").plusSeconds(i));
        e.setId("id_" + i);

        Map<String, Object> payload = new HashMap<>();
        payload.put("temperature", 50 + i);
        payload.put("status", (i % 2 == 0) ? "OK" : "WARN");
        payload.put("meta", Map.of("sequence", i, "source", "ansible"));
        e.setPayload(payload);

        return e;
    }

    @Test
    @Order(1)
    void insertOne_thenFindById() {
        HybridEvent e = sampleEvent(1);

        repo.insertOne(e);

        long count = rawCollection.countDocuments();
        assertEquals(1, count, "Expected exactly one document inserted");

        Document raw = rawCollection.find().first();
        assertNotNull(raw, "Expected to read raw document after insert");

        // Print raw doc to surefire output (shows up in target/surefire-reports)
        System.out.println("RAW DOCUMENT: " + raw.toJson());
        System.out.println("RAW KEYS: " + raw.keySet());

        // Hard assertion: prove what field name was actually stored
        assertTrue(raw.containsKey("id") || raw.containsKey("idValue"),
                "Document did not contain expected id field. Keys: " + raw.keySet());

        // Now try repository lookup
        Optional<HybridEvent> found = repo.findOneById("id_1");
        assertTrue(found.isPresent(),
                "Expected event to be found by id. Raw doc was: " + raw.toJson());

        assertEquals("id_1", found.get().getId());
    }

    @Test
    @Order(2)
    void insertMany_thenFindAll() {
        repo.insertMany(List.of(sampleEvent(1), sampleEvent(2), sampleEvent(3)));
        assertEquals(3, rawCollection.countDocuments());

        List<HybridEvent> all = repo.findAll();
        assertEquals(3, all.size());
    }

    @Test
    @Order(3)
    void findOneByEventTs_exactMatch() {
        HybridEvent e = sampleEvent(10);
        Instant ts = e.getEventTs();

        repo.insertOne(e);

        Optional<HybridEvent> found = repo.findOneByEventTs(ts);
        assertTrue(found.isPresent(), "Expected event to be found by event_ts");
        assertEquals("id_10", found.get().getId());
    }
}