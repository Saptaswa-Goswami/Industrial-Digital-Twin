package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.model.PerformanceMetrics;
import com.industrial.digitaltwin.alertanalytics.model.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("trend-analysis")
@ConditionalOnProperty(name = "app.analytics.mode", havingValue = "trend-analysis")
@Slf4j
public class TrendAnalysisStrategy implements AnalyticsStrategy {

    @Override
    public PerformanceMetrics calculateMetrics(String machineId, List<SensorData> historicalData) {
        log.info("Using trend analysis strategy for machine: {}", machineId);
        // Future implementation with trend analysis
        // For now, delegate to basic strategy as a placeholder
        return new BasicAnalyticsStrategy().calculateMetrics(machineId, historicalData);
    }
}