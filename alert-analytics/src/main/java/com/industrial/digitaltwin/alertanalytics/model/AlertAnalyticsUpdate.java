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
public class AlertAnalyticsUpdate {
    
    @JsonProperty("updateType") // ALERT, ANALYTICS, REPORT
    private String updateType;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("payload") // AlertEvent, PerformanceMetrics, or MaintenanceReport
    private Object payload;
    
    @JsonProperty("severity") // For alert updates
    private String severity;
    
    @JsonProperty("machineId") // Associated machine ID
    private String machineId;
}