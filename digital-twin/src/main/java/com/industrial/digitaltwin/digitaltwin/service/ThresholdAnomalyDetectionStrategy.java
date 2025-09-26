package com.industrial.digitaltwin.digitaltwin.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.industrial.digitaltwin.digitaltwin.config.StateStoreConfig;
import com.industrial.digitaltwin.digitaltwin.model.AlertSeverity;
import com.industrial.digitaltwin.digitaltwin.model.AnomalyRecord;
import com.industrial.digitaltwin.digitaltwin.model.AnomalyType;
import com.industrial.digitaltwin.digitaltwin.model.DigitalTwinState;
import com.industrial.digitaltwin.digitaltwin.model.MachineStatus;
import com.industrial.digitaltwin.digitaltwin.model.MachineTelemetry;
import com.industrial.digitaltwin.digitaltwin.model.SensorData;

import lombok.extern.slf4j.Slf4j;

@Component("threshold-based")
@Slf4j
public class ThresholdAnomalyDetectionStrategy implements AnomalyDetectionStrategy {

    @Autowired
    private StateStoreConfig config;

    @Override
    public List<AnomalyRecord> detect(DigitalTwinState currentState, MachineTelemetry newTelemetry) {
        List<AnomalyRecord> anomalies = new ArrayList<>();
        
        // Adjust thresholds based on current status
        double thresholdMultiplier = getThresholdMultiplierForStatus(currentState.getStatus());
        
        // Threshold-based anomaly detection with status-aware thresholds
        anomalies.addAll(checkTemperatureAnomalies(currentState, newTelemetry, thresholdMultiplier));
        anomalies.addAll(checkVibrationAnomalies(currentState, newTelemetry, thresholdMultiplier));
        anomalies.addAll(checkLoadAnomalies(currentState, newTelemetry, thresholdMultiplier));
        anomalies.addAll(checkPressureAnomalies(currentState, newTelemetry, thresholdMultiplier));
        anomalies.addAll(checkRpmAnomalies(currentState, newTelemetry, thresholdMultiplier));
        
        // Statistical anomaly detection
        anomalies.addAll(checkStatisticalAnomalies(currentState, newTelemetry));
        
        return anomalies;
    }

    @Override
    public String getStrategyName() {
        return "threshold-based";
    }

    private double getThresholdMultiplierForStatus(MachineStatus status) {
        // Adjust sensitivity based on machine status
        switch (status) {
            case CRITICAL:
                return 0.8; // Lower thresholds when critical to catch more anomalies
            case WARNING:
                return 0.9; // Slightly lower thresholds when warning
            case ERROR:
                return 0.7; // Much lower thresholds when in error state
            case MAINTENANCE:
                return 1.2; // Higher thresholds during maintenance (more tolerance)
            case OFFLINE:
                return 1.5; // Even higher thresholds when offline
            case IDLE:
                return 1.1; // Slightly higher thresholds when idle
            case PEAK_LOAD:
                return 0.95; // Lower thresholds when at peak load
            case NORMAL:
            default:
                return 1.0; // Normal thresholds
        }
    }

    private List<AnomalyRecord> checkTemperatureAnomalies(DigitalTwinState currentState, MachineTelemetry telemetry, double thresholdMultiplier) {
        List<AnomalyRecord> anomalies = new ArrayList<>();
        double temperature = telemetry.getSensorData().getTemperature();
        double threshold = config.getAnomalyDetection().getTemperatureThreshold() * thresholdMultiplier;
        double lowerThreshold = 0 * thresholdMultiplier; // Apply multiplier to lower bound as well
        
        if (temperature > threshold) {
            anomalies.add(AnomalyRecord.builder()
                .machineId(telemetry.getMachineId())
                .type(AnomalyType.TEMPERATURE_HIGH)
                .severity(AlertSeverity.CRITICAL)
                .timestamp(telemetry.getTimestamp())
                .description("Temperature exceeds threshold")
                .value(temperature)
                .threshold(threshold)
                .build());
        } else if (temperature < lowerThreshold) { // Assuming negative temperature is anomalous
            anomalies.add(AnomalyRecord.builder()
                .machineId(telemetry.getMachineId())
                .type(AnomalyType.TEMPERATURE_LOW)
                .severity(AlertSeverity.WARNING)
                .timestamp(telemetry.getTimestamp())
                .description("Temperature below normal range")
                .value(temperature)
                .threshold(lowerThreshold)
                .build());
        }
        
        return anomalies;
    }

    private List<AnomalyRecord> checkVibrationAnomalies(DigitalTwinState currentState, MachineTelemetry telemetry, double thresholdMultiplier) {
        List<AnomalyRecord> anomalies = new ArrayList<>();
        double vibration = telemetry.getSensorData().getVibration();
        double threshold = config.getAnomalyDetection().getVibrationThreshold() * thresholdMultiplier;
        double lowerThreshold = 0 * thresholdMultiplier; // Apply multiplier to lower bound as well
        
        if (vibration > threshold) {
            anomalies.add(AnomalyRecord.builder()
                .machineId(telemetry.getMachineId())
                .type(AnomalyType.VIBRATION_HIGH)
                .severity(AlertSeverity.CRITICAL)
                .timestamp(telemetry.getTimestamp())
                .description("Vibration exceeds threshold")
                .value(vibration)
                .threshold(threshold)
                .build());
        } else if (vibration < lowerThreshold) { // Assuming negative vibration is anomalous
            anomalies.add(AnomalyRecord.builder()
                .machineId(telemetry.getMachineId())
                .type(AnomalyType.VIBRATION_LOW)
                .severity(AlertSeverity.WARNING)
                .timestamp(telemetry.getTimestamp())
                .description("Vibration below normal range")
                .value(vibration)
                .threshold(lowerThreshold)
                .build());
        }
        
        return anomalies;
    }

    private List<AnomalyRecord> checkLoadAnomalies(DigitalTwinState currentState, MachineTelemetry telemetry, double thresholdMultiplier) {
        List<AnomalyRecord> anomalies = new ArrayList<>();
        double load = telemetry.getSensorData().getLoad();
        double threshold = config.getAnomalyDetection().getLoadThreshold() * thresholdMultiplier;
        double lowerThreshold = 0 * thresholdMultiplier; // Apply multiplier to lower bound as well
        
        if (load > threshold) {
            anomalies.add(AnomalyRecord.builder()
                .machineId(telemetry.getMachineId())
                .type(AnomalyType.LOAD_HIGH)
                .severity(AlertSeverity.CRITICAL)
                .timestamp(telemetry.getTimestamp())
                .description("Load exceeds threshold")
                .value(load)
                .threshold(threshold)
                .build());
        } else if (load < lowerThreshold) { // Assuming negative load is anomalous
            anomalies.add(AnomalyRecord.builder()
                .machineId(telemetry.getMachineId())
                .type(AnomalyType.LOAD_LOW)
                .severity(AlertSeverity.WARNING)
                .timestamp(telemetry.getTimestamp())
                .description("Load below normal range")
                .value(load)
                .threshold(lowerThreshold)
                .build());
        }
        
        return anomalies;
    }

    private List<AnomalyRecord> checkPressureAnomalies(DigitalTwinState currentState, MachineTelemetry telemetry, double thresholdMultiplier) {
        List<AnomalyRecord> anomalies = new ArrayList<>();
        double pressure = telemetry.getSensorData().getPressure();
        double lowerThreshold = 0 * thresholdMultiplier; // Apply multiplier to lower bound
        double upperThreshold = 15 * thresholdMultiplier; // Apply multiplier to upper bound
        
        if (pressure < lowerThreshold) { // Assuming negative pressure is anomalous
            anomalies.add(AnomalyRecord.builder()
                .machineId(telemetry.getMachineId())
                .type(AnomalyType.PRESSURE_LOW)
                .severity(AlertSeverity.WARNING)
                .timestamp(telemetry.getTimestamp())
                .description("Pressure below normal range")
                .value(pressure)
                .threshold(lowerThreshold)
                .build());
        } else if (pressure > upperThreshold) { // Assuming 15 is a high threshold for pressure
            anomalies.add(AnomalyRecord.builder()
                .machineId(telemetry.getMachineId())
                .type(AnomalyType.PRESSURE_HIGH)
                .severity(AlertSeverity.CRITICAL)
                .timestamp(telemetry.getTimestamp())
                .description("Pressure exceeds normal range")
                .value(pressure)
                .threshold(upperThreshold)
                .build());
        }
        
        return anomalies;
    }

    private List<AnomalyRecord> checkRpmAnomalies(DigitalTwinState currentState, MachineTelemetry telemetry, double thresholdMultiplier) {
        List<AnomalyRecord> anomalies = new ArrayList<>();
        double rpm = telemetry.getSensorData().getRpm();
        double lowerThreshold = 0 * thresholdMultiplier; // Apply multiplier to lower bound
        double upperThreshold = 300 * thresholdMultiplier; // Apply multiplier to upper bound
        
        if (rpm < lowerThreshold) { // Assuming negative RPM is anomalous
            anomalies.add(AnomalyRecord.builder()
                .machineId(telemetry.getMachineId())
                .type(AnomalyType.RPM_LOW)
                .severity(AlertSeverity.WARNING)
                .timestamp(telemetry.getTimestamp())
                .description("RPM below normal range")
                .value(rpm)
                .threshold(lowerThreshold)
                .build());
        } else if (rpm > upperThreshold) { // Assuming 3000 is a high threshold for RPM
            anomalies.add(AnomalyRecord.builder()
                .machineId(telemetry.getMachineId())
                .type(AnomalyType.RPM_HIGH)
                .severity(AlertSeverity.CRITICAL)
                .timestamp(telemetry.getTimestamp())
                .description("RPM exceeds normal range")
                .value(rpm)
                .threshold(upperThreshold)
                .build());
        }
        
        return anomalies;
    }

    /**
     * Statistical anomaly detection using standard deviation from historical data
     */
    private List<AnomalyRecord> checkStatisticalAnomalies(DigitalTwinState currentState, MachineTelemetry telemetry) {
        List<AnomalyRecord> anomalies = new ArrayList<>();
        
        if (currentState.getHistoricalData() != null && currentState.getHistoricalData().size() > 5) {
            SensorData currentData = telemetry.getSensorData();
            
            // Calculate statistical anomalies for temperature
            checkStatisticalAnomalyForValue(
                currentState,
                currentData.getTemperature(),
                currentState.getHistoricalData().stream()
                    .mapToDouble(SensorData::getTemperature)
                    .toArray(),
                AnomalyType.TEMPERATURE_HIGH,
                AnomalyType.TEMPERATURE_LOW,
                "Temperature",
                anomalies,
                telemetry.getTimestamp()
            );
            
            // Calculate statistical anomalies for vibration
            checkStatisticalAnomalyForValue(
                currentState,
                currentData.getVibration(),
                currentState.getHistoricalData().stream()
                    .mapToDouble(SensorData::getVibration)
                    .toArray(),
                AnomalyType.VIBRATION_HIGH,
                AnomalyType.VIBRATION_LOW,
                "Vibration",
                anomalies,
                telemetry.getTimestamp()
            );
            
            // Calculate statistical anomalies for load
            checkStatisticalAnomalyForValue(
                currentState,
                currentData.getLoad(),
                currentState.getHistoricalData().stream()
                    .mapToDouble(SensorData::getLoad)
                    .toArray(),
                AnomalyType.LOAD_HIGH,
                AnomalyType.LOAD_LOW,
                "Load",
                anomalies,
                telemetry.getTimestamp()
            );
        }
        
        return anomalies;
    }

    private void checkStatisticalAnomalyForValue(
            DigitalTwinState currentState,
            double currentValue,
            double[] historicalValues,
            AnomalyType highAnomalyType,
            AnomalyType lowAnomalyType,
            String valueName,
            List<AnomalyRecord> anomalies,
            java.time.Instant timestamp) {
        
        double mean = calculateMean(historicalValues);
        double stdDev = calculateStandardDeviation(historicalValues, mean);
        double baseMultiplier = config.getAnomalyDetection().getStatisticalDeviationMultiplier();
        double statusMultiplier = getThresholdMultiplierForStatus(currentState.getStatus());
        double adjustedMultiplier = baseMultiplier * statusMultiplier; // Apply status-based sensitivity adjustment
        
        double upperThreshold = mean + (adjustedMultiplier * stdDev);
        double lowerThreshold = mean - (adjustedMultiplier * stdDev);
        
        if (currentValue > upperThreshold) {
            anomalies.add(AnomalyRecord.builder()
                .machineId(currentState.getMachineId())
                .type(highAnomalyType)
                .severity(AlertSeverity.WARNING) // Statistical anomalies might be less critical than threshold
                .timestamp(timestamp)
                .description(valueName + " is statistically high (above " + adjustedMultiplier + " standard deviations)")
                .value(currentValue)
                .threshold(upperThreshold)
                .build());
        } else if (currentValue < lowerThreshold) {
            anomalies.add(AnomalyRecord.builder()
                .machineId(currentState.getMachineId())
                .type(lowAnomalyType)
                .severity(AlertSeverity.WARNING) // Statistical anomalies might be less critical than threshold
                .timestamp(timestamp)
                .description(valueName + " is statistically low (below " + adjustedMultiplier + " standard deviations)")
                .value(currentValue)
                .threshold(lowerThreshold)
                .build());
        }
    }

    private double calculateMean(double[] values) {
        return java.util.Arrays.stream(values).average().orElse(0.0);
    }

    private double calculateStandardDeviation(double[] values, double mean) {
        double sum = java.util.Arrays.stream(values)
            .map(value -> Math.pow(value - mean, 2))
            .sum();
        return Math.sqrt(sum / values.length);
    }
}