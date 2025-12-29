package com.example.cdc.model;

import java.time.Instant;

/**
 * Represents a CDC event received from Debezium via Kafka.
 */
public class CdcEvent {

    private String id;
    private String table;
    private String operation; // CREATE, UPDATE, DELETE
    private Object payload;
    private Instant timestamp;
    private long offset;

    public CdcEvent() {
        this.timestamp = Instant.now();
    }

    public CdcEvent(String table, String operation, Object payload) {
        this();
        this.table = table;
        this.operation = operation;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}
