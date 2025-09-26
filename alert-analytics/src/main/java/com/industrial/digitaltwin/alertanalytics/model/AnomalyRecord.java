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
public class AnomalyRecord {
    
    @JsonProperty("machineId")
    private String machineId;
    
    @JsonProperty("anomalyType")
    private AlertType anomalyType;
    
    @JsonProperty("severity")
    private AlertSeverity severity;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("value")
    private double value;
    
    @JsonProperty("threshold")
    private double threshold;
}