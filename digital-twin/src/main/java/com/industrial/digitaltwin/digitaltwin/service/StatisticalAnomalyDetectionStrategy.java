package com.industrial.digitaltwin.digitaltwin.service;

import com.industrial.digitaltwin.digitaltwin.model.AnomalyRecord;
import com.industrial.digitaltwin.digitaltwin.model.DigitalTwinState;
import com.industrial.digitaltwin.digitaltwin.model.MachineTelemetry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component("statistical")
@ConditionalOnProperty(name = "app.anomaly-detection.mode", havingValue = "statistical")
public class StatisticalAnomalyDetectionStrategy implements AnomalyDetectionStrategy {

    @Override
    public List<AnomalyRecord> detect(DigitalTwinState currentState, MachineTelemetry newTelemetry) {
        // Future implementation with statistical methods
        // For now, return empty list as placeholder
        return Collections.emptyList();
    }

    @Override
    public String getStrategyName() {
        return "statistical";
    }
}