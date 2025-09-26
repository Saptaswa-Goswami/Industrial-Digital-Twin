package com.industrial.digitaltwin.digitaltwin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {
    private double temperature;
    private double vibration;
    private double load;
    private double pressure;
    private double rpm;
}