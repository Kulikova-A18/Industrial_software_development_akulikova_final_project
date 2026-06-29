package com.orbitamarket.orders.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "product_type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ArchivePayload.class, name = "ARCHIVE"),
    @JsonSubTypes.Type(value = TaskingPayload.class, name = "TASKING"),
    @JsonSubTypes.Type(value = MonitoringPayload.class, name = "MONITORING")
})
public abstract class OrderPayload {
}

// Archive Payload
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class ArchivePayload extends OrderPayload {
    private String aoi;
    private String captureDate;
    private String sensorType;
}

// Tasking Payload
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class TaskingPayload extends OrderPayload {
    private String aoi;
    private String timeWindowFrom;
    private String timeWindowTo;
    private String sensorType;
}

// Monitoring Payload
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class MonitoringPayload extends OrderPayload {
    private String aoi;
    private String cadence; // DAILY or WEEKLY
    private Integer durationDays;
}