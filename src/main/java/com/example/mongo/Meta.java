package com.example.mongo;

import org.bson.codecs.pojo.annotations.BsonProperty;

public class Meta {
    @BsonProperty("sequence")
    private int sequence;

    @BsonProperty("source")
    private String source;

    public Meta() {}

    public Meta(int sequence, String source) {
        this.sequence = sequence;
        this.source = source;
    }

    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
