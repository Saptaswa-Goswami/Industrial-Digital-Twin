package com.industrial.digitaltwin.devicesimulator.model;

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
public class MachineData {
    
    @JsonProperty("machineId")
    private String machineId;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("sensorData")
    private SensorData sensorData;
    
    @JsonProperty("status")
    private MachineStatus status;
    
    @JsonProperty("additionalMetrics")
    private Map<String, Object> additionalMetrics;
}