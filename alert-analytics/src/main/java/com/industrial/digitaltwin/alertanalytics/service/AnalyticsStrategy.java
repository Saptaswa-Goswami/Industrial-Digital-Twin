package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.model.PerformanceMetrics;
import com.industrial.digitaltwin.alertanalytics.model.SensorData;

import java.util.List;

public interface AnalyticsStrategy {
    PerformanceMetrics calculateMetrics(String machineId, List<SensorData> historicalData);
}