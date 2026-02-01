package com.example.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import org.bson.types.ObjectId;  //rkb not sure this is needed

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ServerApp {

    //rkb private static final ObjectMapper MAPPER = new ObjectMapper();

    private static ObjectMapper buildMapper() {
        return new ObjectMapper()
        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    public static void main(String[] args) {
        Config cfg = new Config();

        // ObjectMapper mapper = new ObjectMapper()
        // .registerModule(new JavaTimeModule())
        // .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ObjectMapper mapper = buildMapper();

        // DO NOT use try-with-resources here.
        MongoClientProvider provider = new MongoClientProvider(cfg);
        HybridEventRepository repo = new HybridEventRepository(provider.getCollection(cfg));

        // Javalin app = Javalin.create(j -> j.http.defaultContentType = "application/json")
        //         .start(cfg.bindHost, cfg.httpPort);

        // Javalin app = Javalin.create(config -> {
        //     config.jsonMapper(new JavalinJackson(mapper));
        // }).start(cfg.bindHost, cfg.httpPort);
        
        Javalin app = Javalin.create(j -> {
            // The second argument is required in Javalin 6.x
            // Use "true" to include stack traces in dev error responses, "false" to hide them.
            j.jsonMapper(new JavalinJackson(mapper, true)); // true for dev, false for prod
            j.http.defaultContentType = "application/json";
        }).start(cfg.bindHost, cfg.httpPort);

        // Helpful while developing: return real exception info
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of(
                    "error", e.getClass().getName(),
                    "message", String.valueOf(e.getMessage())
            ));
        });

        //Post and Get Events

        app.get("/health", ctx -> ctx.json(Map.of("ok", true)));

        app.get("/events", ctx -> ctx.json(repo.findAll()));

        app.get("/events/by-id/{id}", ctx -> {
            String id = ctx.pathParam("id");
            var found = repo.findOneById(id);
            if (found.isEmpty()) { ctx.status(404).json(Map.of("error", "not found")); return; }
            ctx.json(found.get());
        });

        app.get("/events/by-mongoid/{mongoId}", ctx -> {
            String mongoId = ctx.pathParam("mongoId");
            try { new ObjectId(mongoId); }
            catch (Exception e) {
                ctx.status(400).json(Map.of("error", "mongoId must be a valid ObjectId hex string"));
                return;
            }
            var found = repo.findOneByMongoId(mongoId);
            if (found.isEmpty()) { ctx.status(404).json(Map.of("error", "not found")); return; }
            ctx.json(found.get());
        });

        app.get("/events/by-event-ts", ctx -> {
            String ts = ctx.queryParam("ts");
            if (ts == null || ts.isBlank()) {
                ctx.status(400).json(Map.of("error", "missing query param 'ts' (ISO-8601 like 2026-01-30T12:34:56Z)"));
                return;
            }
            Instant instant;
            try { instant = Instant.parse(ts); }
            catch (Exception e) {
                ctx.status(400).json(Map.of("error", "invalid ts; use ISO-8601 like 2026-01-30T12:34:56Z"));
                return;
            }

            var found = repo.findOneByEventTs(instant);
            if (found.isEmpty()) { ctx.status(404).json(Map.of("error", "not found")); return; }
            ctx.json(found.get());
        });

        app.post("/events", ctx -> {
            HybridEvent e = mapper.readValue(ctx.body(), HybridEvent.class);
            if (e.getId() == null || e.getId().isBlank()) {
                ctx.status(400).json(Map.of("error", "Field 'id' is required"));
                return;
            }
            if (e.getEventTs() == null) {
                ctx.status(400).json(Map.of("error", "Field 'event_ts' is required"));
                return;
            }
            repo.insertOne(e);
            ctx.status(201).json(e);
        });

        app.post("/events/batch", ctx -> {
            List<HybridEvent> events = mapper.readValue(
                    ctx.body(),
                    mapper.getTypeFactory().constructCollectionType(List.class, HybridEvent.class)
            );
            repo.insertMany(events);
            ctx.status(201).json(Map.of("inserted", events.size()));
        });

        app.post("/events/import-file", ctx -> {
            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);
            String path = (String) body.get("path");
            if (path == null || path.isBlank()) {
                ctx.status(400).json(Map.of("error", "missing 'path'"));
                return;
            }

            File f = new File(path);
            if (!f.exists() || !f.isFile()) {
                ctx.status(400).json(Map.of("error", "file not found: " + path));
                return;
            }

            List<HybridEvent> events = mapper.readValue(
                    f,
                    mapper.getTypeFactory().constructCollectionType(List.class, HybridEvent.class)
            );
            repo.insertMany(events);
            ctx.json(Map.of("inserted", events.size(), "path", path));
        });

        // Close Mongo when the JVM is shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { provider.close(); } catch (Exception ignored) {}
            try { app.stop(); } catch (Exception ignored) {}
        }));
    }
}
