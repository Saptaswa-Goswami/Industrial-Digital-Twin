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
public class MachineStateUpdate {
    private String machineId;
    private Instant timestamp;
    private SensorData currentData;
    private MachineStatus status;
    private boolean isAnomalyDetected;
    private String machineType;
}