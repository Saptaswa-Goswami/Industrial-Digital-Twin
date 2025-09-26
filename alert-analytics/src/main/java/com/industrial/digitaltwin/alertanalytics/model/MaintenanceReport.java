package com.industrial.digitaltwin.alertanalytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceReport {
    
    @JsonProperty("reportId")
    private String reportId;
    
    @JsonProperty("reportType")
    private ReportType reportType; // DAILY, WEEKLY, MONTHLY
    
    @JsonProperty("generatedTime")
    private Instant generatedTime;
    
    @JsonProperty("machineId")
    private String machineId;
    
    @JsonProperty("content")
    private Map<String, Object> content; // Report content in JSON format
    
    @JsonProperty("metricsSummary")
    private Map<String, Object> metricsSummary; // Summary of key metrics
    
    @JsonProperty("recommendations")
    private List<String> recommendations; // Maintenance recommendations
}