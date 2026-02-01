package com.example.mongo;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class ProducerMain {

    public static void main(String[] args) {
        Config cfg = new Config();

        int count = (args.length > 0) ? Integer.parseInt(args[0]) : -1; // -1 = forever
        long throttleSeconds = (args.length > 1) ? Long.parseLong(args[1]) : 1;

        try (MongoClientProvider provider = new MongoClientProvider(cfg)) {

            // Typed events collection (POJO)
            MongoCollection<HybridEvent> events = provider.getCollection(cfg);

            // Raw counters collection (Document)
            MongoCollection<Document> counters =
                    provider.getRawCollection(cfg.mongoDb, "counters");

            HybridEventProducer producer = new HybridEventProducer(events, counters);

            if (count < 0) producer.runForever(throttleSeconds);
            else producer.runNTimes(count, throttleSeconds);
        }
    }
}
