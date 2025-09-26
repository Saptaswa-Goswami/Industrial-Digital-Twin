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
public class AnomalyRecord {
    private String machineId;
    private AnomalyType type;
    private AlertSeverity severity;
    private Instant timestamp;
    private String description;
    private double value;
    private double threshold;
}