package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.exception.AnalyticsCalculationException;
import com.industrial.digitaltwin.alertanalytics.exception.DataPersistenceException;
import com.industrial.digitaltwin.alertanalytics.model.PerformanceMetrics;
import com.industrial.digitaltwin.alertanalytics.model.SensorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    @Value("${app.analytics.mode:basic}")
    private String analyticsMode;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("basic")
    private AnalyticsStrategy basicStrategy;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("predictive")
    private AnalyticsStrategy predictiveStrategy;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("trend-analysis")
    private AnalyticsStrategy trendAnalysisStrategy;

    private final MetricsService metricsService;
    private final Map<String, List<SensorData>> historicalDataCache = new ConcurrentHashMap<>();

    public PerformanceMetrics calculatePerformanceMetrics(String machineId, List<SensorData> historicalData) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate input
            if (machineId == null || machineId.isEmpty()) {
                throw new AnalyticsCalculationException("Machine ID cannot be null or empty");
            }
            
            if (historicalData == null) {
                throw new AnalyticsCalculationException("Historical data cannot be null");
            }

            AnalyticsStrategy selectedStrategy = getActiveStrategy();
            
            if (selectedStrategy == null) {
                log.error("No analytics strategy available, using basic strategy as fallback");
                selectedStrategy = new BasicAnalyticsStrategy();
            }

            log.debug("Using analytics strategy: {} for machine: {}",
                    selectedStrategy.getClass().getSimpleName(), machineId);

            PerformanceMetrics metrics = selectedStrategy.calculateMetrics(machineId, historicalData);
            
            // Record metrics
            long calculationTime = System.currentTimeMillis() - startTime;
            metricsService.incrementAnalyticsCalculations();
            metricsService.recordAnalyticsCalculationTime(calculationTime);
            
            log.info("Performance metrics calculated for machine: {} in {} ms", machineId, calculationTime);
            
            return metrics;
        } catch (AnalyticsCalculationException e) {
            // Re-throw our custom exception
            throw e;
        } catch (Exception e) {
            log.error("Error calculating performance metrics for machine: {}", machineId, e);
            throw new AnalyticsCalculationException("Failed to calculate performance metrics", e);
        }
    }

    private AnalyticsStrategy getActiveStrategy() {
        switch (analyticsMode) {
            case "predictive":
                if (predictiveStrategy != null) {
                    return predictiveStrategy;
                }
                log.warn("Predictive analytics mode requested but not available, falling back to basic");
                // Fall through to basic
            case "trend-analysis":
                if (trendAnalysisStrategy != null) {
                    return trendAnalysisStrategy;
                }
                log.warn("Trend analysis mode requested but not available, falling back to basic");
                // Fall through to basic
            case "basic":
            default:
                if (basicStrategy != null) {
                    return basicStrategy;
                } else {
                    log.error("Basic analytics strategy is not available!");
                    return new BasicAnalyticsStrategy();
                }
        }
    }

    public void updateHistoricalData(String machineId, SensorData sensorData) {
        if (machineId == null || machineId.isEmpty()) {
            throw new DataPersistenceException("Machine ID cannot be null or empty");
        }
        
        if (sensorData == null) {
            throw new DataPersistenceException("Sensor data cannot be null");
        }

        try {
            historicalDataCache.computeIfAbsent(machineId, k -> new java.util.ArrayList<>()).add(sensorData);
            // Keep only recent data to prevent memory issues
            List<SensorData> data = historicalDataCache.get(machineId);
            if (data.size() > 1000) { // Keep last 1000 data points
                historicalDataCache.put(machineId, data.subList(data.size() - 500, data.size()));
            }
            
            // Record metrics
            metricsService.incrementStateUpdates();
        } catch (Exception e) {
            log.error("Failed to update historical data for machine: {}", machineId, e);
            throw new DataPersistenceException("Failed to update historical data", e);
        }
    }

    public List<SensorData> getHistoricalData(String machineId) {
        if (machineId == null || machineId.isEmpty()) {
            throw new DataPersistenceException("Machine ID cannot be null or empty");
        }
        
        try {
            return historicalDataCache.getOrDefault(machineId, List.of());
        } catch (Exception e) {
            log.error("Failed to retrieve historical data for machine: {}", machineId, e);
            throw new DataPersistenceException("Failed to retrieve historical data", e);
        }
    }

    public Map<String, Object> generateAnalyticsReport(String machineId) {
        if (machineId == null || machineId.isEmpty()) {
            throw new AnalyticsCalculationException("Machine ID cannot be null or empty");
        }

        try {
            List<SensorData> historicalData = getHistoricalData(machineId);
            PerformanceMetrics metrics = calculatePerformanceMetrics(machineId, historicalData);

            // Generate a comprehensive analytics report
            Map<String, Object> report = new java.util.HashMap<>();
            report.put("machineId", machineId);
            report.put("reportGeneratedAt", Instant.now());
            report.put("performanceMetrics", metrics);
            report.put("dataPointsCount", historicalData.size());
            
            // Add summary statistics
            if (!historicalData.isEmpty()) {
                double avgLoad = historicalData.stream().mapToDouble(SensorData::getLoad).average().orElse(0.0);
                double avgTemp = historicalData.stream().mapToDouble(SensorData::getTemperature).average().orElse(0.0);
                double avgVibration = historicalData.stream().mapToDouble(SensorData::getVibration).average().orElse(0.0);
                
                report.put("averageLoad", avgLoad);
                report.put("averageTemperature", avgTemp);
                report.put("averageVibration", avgVibration);
                
                // Add trend analysis
                if (metrics.getTrends() != null && !metrics.getTrends().isEmpty()) {
                    report.put("trends", metrics.getTrends());
                }
            }
            
            return report;
        } catch (AnalyticsCalculationException | DataPersistenceException e) {
            // Re-throw our custom exceptions
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate analytics report for machine: {}", machineId, e);
            throw new AnalyticsCalculationException("Failed to generate analytics report", e);
        }
    }
    
    // Methods required by the controller
    public List<PerformanceMetrics> getAllPerformanceMetrics() {
        // For now, return an empty list or implement based on your requirements
        // In a real implementation, you would fetch all metrics from the database
        return List.of();
    }
    
    public java.util.Optional<PerformanceMetrics> getPerformanceMetrics(String machineId) {
        // For now, return empty optional or implement based on your requirements
        // In a real implementation, you would fetch metrics for a specific machine from the database
        return java.util.Optional.empty();
    }
    
    public List<PerformanceMetrics> getHistoricalPerformanceMetrics(String machineId, Long fromTime, Long toTime) {
        // For now, return an empty list or implement based on your requirements
        // In a real implementation, you would fetch historical metrics from the database
        return List.of();
    }

    // New methods to support historical analytics
    public List<PerformanceMetrics> getHistoricalPerformanceMetrics(String machineId, java.time.Instant fromTime, java.time.Instant toTime) {
        // Fetch historical performance metrics from the database based on time range
        // This would be implemented with a repository method to query the PerformanceMetricsEntity table
        return List.of(); // Placeholder - implement with actual repository query
    }

    public PerformanceMetrics getHistoricalPerformanceAtTime(String machineId, java.time.Instant time) {
        // Get the performance metrics closest to the specified time
        // This would be implemented with a repository method to query the PerformanceMetricsEntity table
        return null; // Placeholder - implement with actual repository query
    }

    public void calculateAndStoreHistoricalMetrics(String machineId, java.time.Instant startTime, java.time.Instant endTime) {
        // Calculate performance metrics for a historical time range and store them
        // This would process historical data and store the results in the database
        log.info("Calculating historical metrics for machine: {} from: {} to: {}", machineId, startTime, endTime);
    }
    
    public Object getAllTrends() {
        // For now, return an empty object or implement based on your requirements
        return new java.util.HashMap<>();
    }
    
    public Object getTrendsByMachineId(String machineId) {
        // For now, return an empty object or implement based on your requirements
        return new java.util.HashMap<>();
    }
}