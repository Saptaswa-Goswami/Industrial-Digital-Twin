package com.industrial.digitaltwin.alertanalytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {
    
    @JsonProperty("machineId")
    private String machineId;
    
    @JsonProperty("calculationTime")
    private Instant calculationTime;
    
    @JsonProperty("averageLoad")
    private double averageLoad;
    
    @JsonProperty("averageTemperature")
    private double averageTemperature;
    
    @JsonProperty("averageVibration")
    private double averageVibration;
    
    @JsonProperty("uptimePercentage")
    private double uptimePercentage;
    
    @JsonProperty("anomalyCount")
    private int anomalyCount;
    
    @JsonProperty("efficiencyRating")
    private double efficiencyRating;
    
    @JsonProperty("averageDowntime")
    private Duration averageDowntime;
    
    @JsonProperty("trends")
    private List<Trend> trends; // Historical trend data
}