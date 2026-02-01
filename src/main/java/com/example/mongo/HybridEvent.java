package com.example.mongo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Map;

/**
 * HybridEvent - schema-validator safe:
 * BSON fields: _id, event_ts, id, payload
 * JSON fields: mongoId, event_ts, id, payload
 */
public class HybridEvent {

    // Mongo _id
    @BsonId
    @JsonIgnore
    private ObjectId mongoObjectId;

    public ObjectId getMongoObjectId() { return mongoObjectId; }
    public void setMongoObjectId(ObjectId mongoObjectId) { this.mongoObjectId = mongoObjectId; }

    @JsonGetter("mongoId")
    public String getMongoId() {
        return mongoObjectId == null ? null : mongoObjectId.toHexString();
    }

    @JsonSetter("mongoId")
    public void setMongoId(String hex) {
        if (hex == null || hex.isBlank()) this.mongoObjectId = null;
        else this.mongoObjectId = new ObjectId(hex);
    }

    // event_ts
    @BsonProperty("event_ts")
    @JsonProperty("event_ts")
    @JsonAlias({"eventTs"})
    private Instant eventTs;

    public Instant getEventTs() { return eventTs; }
    public void setEventTs(Instant eventTs) { this.eventTs = eventTs; }

    // id (REQUIRED by your Mongo validator)
    @BsonProperty("id")
    @JsonProperty("id")
    private String id;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // payload
    @JsonProperty("payload")
    private Map<String, Object> payload;

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public HybridEvent() {}
}
