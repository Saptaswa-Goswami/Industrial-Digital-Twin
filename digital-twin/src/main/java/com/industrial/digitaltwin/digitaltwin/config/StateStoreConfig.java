package com.industrial.digitaltwin.digitaltwin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.digital-twin")
@Data
public class StateStoreConfig {

    private Duration stateRetention = Duration.ofHours(24);
    private Duration snapshotInterval = Duration.ofMinutes(5);
    
    private AnomalyDetectionProperties anomalyDetection = new AnomalyDetectionProperties();
    private StateStoreProperties stateStore = new StateStoreProperties();
    
    @Data
    public static class AnomalyDetectionProperties {
        private double temperatureThreshold = 85.0;
        private double vibrationThreshold = 3.0;
        private double loadThreshold = 95.0;
        private double statisticalDeviationMultiplier = 2.5;
    }
    
    @Data
    public static class StateStoreProperties {
        private int maxEntries = 10000;
        private Duration evictionTimeout = Duration.ofHours(1);
    }
}