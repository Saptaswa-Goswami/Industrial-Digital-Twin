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
public class MachineTelemetry {
    private String machineId;
    private Instant timestamp;
    private SensorData sensorData;
    private MachineStatus status;
    private String schemaVersion = "1.0";
    private String dataSourceType = "simulator";
}