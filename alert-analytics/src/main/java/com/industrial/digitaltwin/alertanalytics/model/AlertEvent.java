package com.industrial.digitaltwin.alertanalytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {
    
    @JsonProperty("alertId")
    private String alertId;
    
    @JsonProperty("machineId")
    private String machineId;
    
    @JsonProperty("severity")
    private AlertSeverity severity; // CRITICAL, WARNING, INFO
    
    @JsonProperty("type")
    private AlertType type; // OVERHEATING, EXCESSIVE_VIBRATION, etc.
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("details")
    private Map<String, Object> details;
    
    @JsonProperty("status")
    private AlertStatus status; // NEW, ACKNOWLEDGED, RESOLVED
    
    @JsonProperty("assignedTo")
    private String assignedTo;
    
    @JsonProperty("resolutionTime")
    private Instant resolutionTime;
}