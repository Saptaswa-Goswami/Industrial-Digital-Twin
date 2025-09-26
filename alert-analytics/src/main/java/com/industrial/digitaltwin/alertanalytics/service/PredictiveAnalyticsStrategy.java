package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.model.PerformanceMetrics;
import com.industrial.digitaltwin.alertanalytics.model.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("predictive")
@ConditionalOnProperty(name = "app.analytics.mode", havingValue = "predictive")
@Slf4j
public class PredictiveAnalyticsStrategy implements AnalyticsStrategy {

    @Override
    public PerformanceMetrics calculateMetrics(String machineId, List<SensorData> historicalData) {
        log.info("Using predictive analytics strategy for machine: {}", machineId);
        // Future implementation with ML-based predictions
        // For now, delegate to basic strategy as a placeholder
        return new BasicAnalyticsStrategy().calculateMetrics(machineId, historicalData);
    }
}