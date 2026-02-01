package com.example.mongo;

import org.bson.codecs.pojo.annotations.BsonProperty;

public class Payload {
    @BsonProperty("temperature")
    private int temperature;

    @BsonProperty("status")
    private String status;

    @BsonProperty("meta")
    private Meta meta;

    public Payload() {}

    public Payload(int temperature, String status, Meta meta) {
        this.temperature = temperature;
        this.status = status;
        this.meta = meta;
    }

    public int getTemperature() { return temperature; }
    public void setTemperature(int temperature) { this.temperature = temperature; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Meta getMeta() { return meta; }
    public void setMeta(Meta meta) { this.meta = meta; }
}
