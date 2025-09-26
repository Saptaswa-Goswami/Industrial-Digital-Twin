package com.industrial.digitaltwin.alertanalytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trend {
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("value")
    private double value;
    
    @JsonProperty("type")
    private String type; // e.g., "temperature", "vibration", "load"
    
    @JsonProperty("direction")
    private String direction; // "increasing", "decreasing", "stable"
}