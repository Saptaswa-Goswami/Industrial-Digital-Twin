package com.industrial.digitaltwin.digitaltwin.model;

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
public class DigitalTwinState {
    private String machineId;
    private Instant lastUpdated;
    private SensorData currentSensorData;
    private List<SensorData> historicalData; // Last N readings
    private Map<String, Object> computedMetrics; // Running averages, trends
    private MachineStatus status;
    private List<AnomalyRecord> recentAnomalies;
    private long operationalHours;
    private double efficiencyRating;
}