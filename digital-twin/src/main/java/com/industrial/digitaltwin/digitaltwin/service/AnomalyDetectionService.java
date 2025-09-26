package com.industrial.digitaltwin.digitaltwin.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.industrial.digitaltwin.digitaltwin.model.AnomalyEvent;
import com.industrial.digitaltwin.digitaltwin.model.AnomalyRecord;
import com.industrial.digitaltwin.digitaltwin.model.DigitalTwinState;
import com.industrial.digitaltwin.digitaltwin.model.MachineTelemetry;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnomalyDetectionService {

    @Value("${app.digital-twin.anomaly-detection.mode: threshold-based}")
    private String detectionMode;
    
    @Autowired
    @Qualifier("threshold-based")
    private AnomalyDetectionStrategy thresholdStrategy;
    
    @Autowired(required = false) // Optional - only if bean exists
    @Qualifier("ml-based")
    private AnomalyDetectionStrategy mlStrategy;
    
    @Autowired(required = false) // Optional - only if bean exists
    @Qualifier("statistical")
    private AnomalyDetectionStrategy statisticalStrategy;

    public AnomalyEvent detectAnomalies(DigitalTwinState currentState, MachineTelemetry newTelemetry) {
        // Use the active strategy based on configuration
        AnomalyDetectionStrategy selectedStrategy = getActiveStrategy();
        List<AnomalyRecord> anomalies = selectedStrategy.detect(currentState, newTelemetry);
        
        if (!anomalies.isEmpty()) {
            return AnomalyEvent.builder()
                .machineId(currentState.getMachineId())
                .anomalies(anomalies)
                .timestamp(Instant.now())
                .algorithmUsed(selectedStrategy.getStrategyName())
                .build();
        }
        
        return null;
    }
    
    private AnomalyDetectionStrategy getActiveStrategy() {
        switch (detectionMode) {
            case "ml-based":
                if (mlStrategy != null) return mlStrategy;
                // Fall back to threshold if ML not configured
            case "statistical":
                if (statisticalStrategy != null) return statisticalStrategy;
                // Fall back to threshold if statistical not configured
            default:
                return thresholdStrategy;
        }
    }
}