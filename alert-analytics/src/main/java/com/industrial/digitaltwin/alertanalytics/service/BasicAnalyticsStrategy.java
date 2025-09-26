package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.model.PerformanceMetrics;
import com.industrial.digitaltwin.alertanalytics.model.SensorData;
import com.industrial.digitaltwin.alertanalytics.model.Trend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

@Component("basic")
@Slf4j
public class BasicAnalyticsStrategy implements AnalyticsStrategy {

    @Override
    public PerformanceMetrics calculateMetrics(String machineId, List<SensorData> historicalData) {
        if (historicalData == null || historicalData.isEmpty()) {
            log.warn("No historical data available for machine: {}", machineId);
            return createDefaultMetrics(machineId);
        }

        // Calculate various performance indicators
        double averageLoad = calculateAverage(historicalData, SensorData::getLoad);
        double averageTemperature = calculateAverage(historicalData, SensorData::getTemperature);
        double averageVibration = calculateAverage(historicalData, SensorData::getVibration);

        // Calculate standard deviations for variability analysis
        double loadStdDev = calculateStandardDeviation(historicalData, SensorData::getLoad, averageLoad);
        double tempStdDev = calculateStandardDeviation(historicalData, SensorData::getTemperature, averageTemperature);
        double vibrationStdDev = calculateStandardDeviation(historicalData, SensorData::getVibration, averageVibration);

        // Calculate uptime based on operational status (for now, assume all data points represent operational time)
        double uptimePercentage = 100.0; // Placeholder - would need actual status data

        // Count anomalies in the period (placeholder - would need actual anomaly data)
        int anomalyCount = calculateAnomalyCount(historicalData);

        // Calculate efficiency rating
        double efficiencyRating = calculateEfficiencyRating(averageLoad, uptimePercentage, anomalyCount);

        // Calculate trends
        List<Trend> trends = calculateTrends(historicalData);

        // Calculate min/max values
        double minLoad = historicalData.stream().mapToDouble(SensorData::getLoad).min().orElse(0.0);
        double maxLoad = historicalData.stream().mapToDouble(SensorData::getLoad).max().orElse(0.0);
        double minTemp = historicalData.stream().mapToDouble(SensorData::getTemperature).min().orElse(0.0);
        double maxTemp = historicalData.stream().mapToDouble(SensorData::getTemperature).max().orElse(0.0);
        double minVibration = historicalData.stream().mapToDouble(SensorData::getVibration).min().orElse(0.0);
        double maxVibration = historicalData.stream().mapToDouble(SensorData::getVibration).max().orElse(0.0);

        return PerformanceMetrics.builder()
                .machineId(machineId)
                .calculationTime(Instant.now())
                .averageLoad(averageLoad)
                .averageTemperature(averageTemperature)
                .averageVibration(averageVibration)
                .uptimePercentage(uptimePercentage)
                .anomalyCount(anomalyCount)
                .efficiencyRating(efficiencyRating)
                .averageDowntime(Duration.ZERO) // Placeholder
                .trends(trends)
                .build();
    }

    private double calculateAverage(List<SensorData> data, ToDoubleFunction<SensorData> extractor) {
        return data.stream()
                .mapToDouble(extractor)
                .average()
                .orElse(0.0);
    }

    private double calculateStandardDeviation(List<SensorData> data, ToDoubleFunction<SensorData> extractor, double mean) {
        if (data.size() < 2) return 0.0;
        
        double sumSquaredDiffs = data.stream()
            .mapToDouble(extractor)
            .map(value -> Math.pow(value - mean, 2))
            .sum();
            
        return Math.sqrt(sumSquaredDiffs / (data.size() - 1));
    }

    private int calculateAnomalyCount(List<SensorData> historicalData) {
        // Simple anomaly detection based on thresholds
        int count = 0;
        for (SensorData data : historicalData) {
            if (data.getTemperature() > 90.0 || // High temperature threshold
                data.getVibration() > 3.0 ||   // High vibration threshold
                data.getLoad() > 95.0) {       // High load threshold
                count++;
            }
        }
        return count;
    }

    private double calculateEfficiencyRating(double averageLoad, double uptimePercentage, int anomalyCount) {
        // Simple efficiency calculation - can be enhanced based on business requirements
        double loadFactor = Math.min(averageLoad / 100.0, 1.0); // Normalize load to 0-1
        double uptimeFactor = uptimePercentage / 100.0; // Normalize uptime to 0-1
        double anomalyPenalty = Math.min(anomalyCount * 0.05, 0.5); // Max 50% penalty for anomalies

        // Calculate efficiency as a weighted combination
        return Math.max((loadFactor * 0.4 + uptimeFactor * 0.4) * 10 - (anomalyPenalty * 100), 0);
    }

    private List<Trend> calculateTrends(List<SensorData> historicalData) {
        List<Trend> trends = new ArrayList<>();
        
        if (historicalData.size() < 2) {
            return trends; // Need at least 2 points to calculate a trend
        }

        // Calculate temperature trend
        double firstTemp = historicalData.get(0).getTemperature();
        double lastTemp = historicalData.get(historicalData.size() - 1).getTemperature();
        String tempTrendDirection = lastTemp > firstTemp ? "increasing" : (lastTemp < firstTemp ? "decreasing" : "stable");
        trends.add(Trend.builder()
                .timestamp(Instant.now())
                .value((lastTemp - firstTemp) / historicalData.size()) // Rate of change
                .type("temperature")
                .direction(tempTrendDirection)
                .build());

        // Calculate vibration trend
        double firstVib = historicalData.get(0).getVibration();
        double lastVib = historicalData.get(historicalData.size() - 1).getVibration();
        String vibTrendDirection = lastVib > firstVib ? "increasing" : (lastVib < firstVib ? "decreasing" : "stable");
        trends.add(Trend.builder()
                .timestamp(Instant.now())
                .value((lastVib - firstVib) / historicalData.size()) // Rate of change
                .type("vibration")
                .direction(vibTrendDirection)
                .build());

        // Calculate load trend
        double firstLoad = historicalData.get(0).getLoad();
        double lastLoad = historicalData.get(historicalData.size() - 1).getLoad();
        String loadTrendDirection = lastLoad > firstLoad ? "increasing" : (lastLoad < firstLoad ? "decreasing" : "stable");
        trends.add(Trend.builder()
                .timestamp(Instant.now())
                .value((lastLoad - firstLoad) / historicalData.size()) // Rate of change
                .type("load")
                .direction(loadTrendDirection)
                .build());

        return trends;
    }

    private PerformanceMetrics createDefaultMetrics(String machineId) {
        return PerformanceMetrics.builder()
                .machineId(machineId)
                .calculationTime(Instant.now())
                .averageLoad(0.0)
                .averageTemperature(0.0)
                .averageVibration(0.0)
                .uptimePercentage(0.0)
                .anomalyCount(0)
                .efficiencyRating(0.0)
                .averageDowntime(Duration.ZERO)
                .trends(List.of())
                .build();
    }
}