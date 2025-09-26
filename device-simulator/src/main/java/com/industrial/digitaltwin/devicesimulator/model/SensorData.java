package com.industrial.digitaltwin.devicesimulator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {
    
    @JsonProperty("temperature")
    private double temperature;
    
    @JsonProperty("vibration")
    private double vibration;
    
    @JsonProperty("load")
    private double load;
    
    @JsonProperty("pressure")
    private double pressure;
    
    @JsonProperty("rpm")
    private double rpm;
}