package com.industrial.digitaltwin.devicesimulator.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app.simulation")
@Data
@Validated
public class SimulationConfig {

    @Valid
    private List<MachineConfig> machines;

    private long samplingInterval = 5000; // milliseconds

    private double anomalyProbability = 0.05; // 5% chance of anomaly

    private List<String> machineTypes;

    @Data
    public static class MachineConfig {
        @NotBlank
        private String id;

        @NotBlank
        private String type;

        private double baselineTemp = 70.0;

        private double tempVariation = 10.0;

        private double baselineVibration = 1.5;

        private double vibrationVariation = 0.8;

        private String operationalPattern = "continuous"; // continuous, intermittent, scheduled
    }
}