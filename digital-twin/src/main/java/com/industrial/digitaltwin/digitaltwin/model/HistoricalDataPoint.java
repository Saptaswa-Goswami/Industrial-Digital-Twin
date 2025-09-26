package com.industrial.digitaltwin.digitaltwin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalDataPoint {
    private Instant timestamp;
    private SensorData sensorData;
    private double computedValue;
    private String metricType;
}