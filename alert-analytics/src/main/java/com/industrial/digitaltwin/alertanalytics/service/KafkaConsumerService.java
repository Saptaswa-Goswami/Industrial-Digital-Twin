package com.industrial.digitaltwin.alertanalytics.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.industrial.digitaltwin.alertanalytics.model.AlertEvent;
import com.industrial.digitaltwin.alertanalytics.model.MachineTelemetry;
import com.industrial.digitaltwin.alertanalytics.model.SensorData;
import com.industrial.digitaltwin.alertanalytics.repository.HistoricalDataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final AlertProcessingService alertProcessingService;
    private final AnalyticsService analyticsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebSocketBroadcastService webSocketBroadcastService;
    private final HistoricalDataRepository historicalDataRepository;

    @KafkaListener(topics = "anomaly-events", groupId = "alert-analytics-group")
    public void consumeAnomalyEvents(Map<String, Object> rawAnomalyEvent) {
        log.info("Received anomaly event: {}", rawAnomalyEvent);

        try {
            // Convert the raw anomaly event map to our internal AlertEvent model
            AlertEvent alertEvent = convertToAlertEvent(rawAnomalyEvent);

            // Process the alert
            alertProcessingService.processAlert(alertEvent);

            // Publish alert to downstream consumers
            kafkaTemplate.send("alerts", alertEvent.getMachineId(), alertEvent);
            log.info("Alert published to 'alerts' topic: {}", alertEvent.getAlertId());

            // Broadcast alert update via WebSocket
            try {
                webSocketBroadcastService.broadcastAlertUpdate(alertEvent);
            } catch (Exception e) {
                log.error("Error broadcasting alert via WebSocket", e);
            }

        } catch (Exception e) {
            log.error("Error processing anomaly event: {}", rawAnomalyEvent, e);
        }
    }

    @KafkaListener(topics = "machine-state-updates", groupId = "alert-analytics-group")
    public void consumeMachineStateUpdates(Map<String, Object> rawStateUpdate) {
        log.info("Received machine state update: {}", rawStateUpdate);

        try {
            // Convert the raw state update map to our internal MachineTelemetry model
            MachineTelemetry stateUpdate = convertToMachineTelemetry(rawStateUpdate);

            // Store the telemetry data in the historical data repository
            storeHistoricalData(stateUpdate);

            // Update analytics with the new telemetry data
            analyticsService.updateHistoricalData(stateUpdate.getMachineId(), stateUpdate.getSensorData());

            // Calculate performance metrics
            var performanceMetrics = analyticsService.calculatePerformanceMetrics(
                    stateUpdate.getMachineId(),
                    analyticsService.getHistoricalData(stateUpdate.getMachineId()));

            // Publish analytics report
            kafkaTemplate.send("analytics-reports", stateUpdate.getMachineId(), performanceMetrics);
            log.info("Analytics report published for machine: {}", stateUpdate.getMachineId());

            // Broadcast analytics update via WebSocket
            try {
                webSocketBroadcastService.broadcastAnalyticsUpdate(performanceMetrics);
            } catch (Exception e) {
                log.error("Error broadcasting analytics via WebSocket", e);
            }

        } catch (Exception e) {
            log.error("Error processing machine state update: {}", rawStateUpdate, e);
        }
    }

    private AlertEvent convertToAlertEvent(Map<String, Object> rawAnomalyEvent) {
        String machineId = (String) rawAnomalyEvent.get("machineId");
        Object timestampObj = rawAnomalyEvent.get("timestamp");
        Object anomaliesObj = rawAnomalyEvent.get("anomalies");

        // Handle timestamp as either String (ISO format) or numeric (epoch seconds with
        // decimals)
        Instant timestamp = parseTimestamp(timestampObj);

        // Extract the first anomaly for alert creation (in a real scenario, we might
        // create multiple alerts)
        // Check if anomalies is a List or Map and handle accordingly
        Object firstAnomaly = null;
        if (anomaliesObj instanceof java.util.List) {
            java.util.List<?> anomaliesList = (java.util.List<?>) anomaliesObj;
            if (!anomaliesList.isEmpty()) {
                firstAnomaly = anomaliesList.get(0);
            }
        } else if (anomaliesObj instanceof Map) {
            // If it's a single anomaly as a map, use it directly
            firstAnomaly = anomaliesObj;
        }

        // Determine severity and type from the first anomaly if available
        com.industrial.digitaltwin.alertanalytics.model.AlertSeverity severity = com.industrial.digitaltwin.alertanalytics.model.AlertSeverity.WARNING;
        com.industrial.digitaltwin.alertanalytics.model.AlertType type = com.industrial.digitaltwin.alertanalytics.model.AlertType.EXCESSIVE_VIBRATION;
        String description = "Anomaly detected in machine " + machineId;

        if (firstAnomaly instanceof Map) {
            Map<String, Object> firstAnomalyMap = (Map<String, Object>) firstAnomaly;
            String anomalyTypeStr = (String) firstAnomalyMap.get("type");
            String anomalySeverityStr = (String) firstAnomalyMap.get("severity");
            String anomalyDescription = (String) firstAnomalyMap.get("description");

            if (anomalyTypeStr != null) {
                try {
                    type = com.industrial.digitaltwin.alertanalytics.model.AlertType
                            .valueOf(anomalyTypeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown anomaly type: {}, defaulting to EXCESSIVE_VIBRATION", anomalyTypeStr);
                    // Try to map common anomaly types to our alert types
                    switch (anomalyTypeStr.toUpperCase()) {
                        case "TEMPERATURE_HIGH":
                            type = com.industrial.digitaltwin.alertanalytics.model.AlertType.TEMPERATURE_HIGH;
                            break;
                        case "TEMPERATURE_LOW":
                            type = com.industrial.digitaltwin.alertanalytics.model.AlertType.TEMPERATURE_LOW;
                            break;
                        case "VIBRATION_HIGH":
                            type = com.industrial.digitaltwin.alertanalytics.model.AlertType.VIBRATION_HIGH;
                            break;
                        case "VIBRATION_LOW":
                            type = com.industrial.digitaltwin.alertanalytics.model.AlertType.VIBRATION_LOW;
                            break;
                        case "LOAD_HIGH":
                            type = com.industrial.digitaltwin.alertanalytics.model.AlertType.LOAD_HIGH;
                            break;
                        case "LOAD_LOW":
                            type = com.industrial.digitaltwin.alertanalytics.model.AlertType.LOAD_LOW;
                            break;
                        case "PRESSURE_HIGH":
                            type = com.industrial.digitaltwin.alertanalytics.model.AlertType.PRESSURE_HIGH;
                            break;
                        case "PRESSURE_LOW":
                            type = com.industrial.digitaltwin.alertanalytics.model.AlertType.PRESSURE_LOW;
                            break;
                        case "RPM_HIGH":
                            type = com.industrial.digitaltwin.alertanalytics.model.AlertType.RPM_HIGH;
                            break;
                        case "RPM_LOW":
                            type = com.industrial.digitaltwin.alertanalytics.model.AlertType.RPM_LOW;
                            break;
                        default:
                            type = com.industrial.digitaltwin.alertanalytics.model.AlertType.EXCESSIVE_VIBRATION;
                    }
                }
            }

            if (anomalySeverityStr != null) {
                try {
                    severity = com.industrial.digitaltwin.alertanalytics.model.AlertSeverity
                            .valueOf(anomalySeverityStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown severity: {}, defaulting to WARNING", anomalySeverityStr);
                }
            }

            if (anomalyDescription != null) {
                description = anomalyDescription;
            }
        }

        return AlertEvent.builder()
                .alertId("ALERT_" + System.currentTimeMillis()) // Generate alert ID
                .machineId(machineId)
                .severity(severity)
                .type(type)
                .timestamp(timestamp)
                .description(description)
                .details(rawAnomalyEvent) // Include raw details
                .status(com.industrial.digitaltwin.alertanalytics.model.AlertStatus.NEW)
                .build();
    }

    private MachineTelemetry convertToMachineTelemetry(Map<String, Object> rawStateUpdate) {
        String machineId = (String) rawStateUpdate.get("machineId");
        Object timestampObj = rawStateUpdate.get("timestamp");
        
        // The digital twin service sends sensor data in "currentSensorData" field
        Object currentSensorDataObj = rawStateUpdate.get("currentSensorData");
        Map<String, Object> sensorDataMap = null;
        if (currentSensorDataObj instanceof Map) {
            sensorDataMap = (Map<String, Object>) currentSensorDataObj;
        } else {
            // Fallback to "sensorData" for compatibility
            sensorDataMap = (Map<String, Object>) rawStateUpdate.get("sensorData");
        }

        // Handle timestamp as either String (ISO format) or numeric (epoch seconds with
        // decimals)
        Instant timestamp = parseTimestamp(timestampObj);

        // Convert status from device simulator to digital twin status
        String statusStr = (String) rawStateUpdate.get("status");
        com.industrial.digitaltwin.alertanalytics.model.MachineStatus status = convertStatus(statusStr);

        SensorData sensorData = SensorData.builder()
                .temperature(sensorDataMap != null ? ((Number) sensorDataMap.get("temperature")).doubleValue() : 0.0)
                .vibration(sensorDataMap != null ? ((Number) sensorDataMap.get("vibration")).doubleValue() : 0.0)
                .load(sensorDataMap != null ? ((Number) sensorDataMap.get("load")).doubleValue() : 0.0)
                .pressure(sensorDataMap != null ? ((Number) sensorDataMap.get("pressure")).doubleValue() : 0.0)
                .rpm(sensorDataMap != null ? ((Number) sensorDataMap.get("rpm")).doubleValue() : 0.0)
                .build();

        return MachineTelemetry.builder()
                .machineId(machineId)
                .timestamp(timestamp)
                .sensorData(sensorData)
                .status(status)
                .build();
    }

    private Instant parseTimestamp(Object timestampObj) {
        if (timestampObj instanceof String) {
            return Instant.parse((String) timestampObj);
        } else if (timestampObj instanceof Number) {
            // Convert from epoch seconds with decimal to Instant
            double epochSeconds = ((Number) timestampObj).doubleValue();
            long seconds = (long) epochSeconds;
            long nanos = (long) ((epochSeconds - seconds) * 1_000_000_000);
            return Instant.ofEpochSecond(seconds, nanos);
        } else {
            return Instant.now();
        }
    }

    private void storeHistoricalData(MachineTelemetry machineTelemetry) {
        try {
            // Create a HistoricalDataEntity from the MachineTelemetry
            var historicalData = com.industrial.digitaltwin.alertanalytics.entity.HistoricalDataEntity.builder()
                    .machineId(machineTelemetry.getMachineId())
                    .timestamp(machineTelemetry.getTimestamp())
                    .temperature(machineTelemetry.getSensorData().getTemperature())
                    .vibration(machineTelemetry.getSensorData().getVibration())
                    .load(machineTelemetry.getSensorData().getLoad())
                    .pressure(machineTelemetry.getSensorData().getPressure())
                    .rpm(machineTelemetry.getSensorData().getRpm())
                    .status(machineTelemetry.getStatus() != null ? machineTelemetry.getStatus().name() : null)
                    .build();

            // Save to the historical data repository
            historicalDataRepository.save(historicalData);
            log.debug("Stored historical data for machine: {} at time: {}",
                    machineTelemetry.getMachineId(), machineTelemetry.getTimestamp());
        } catch (Exception e) {
            log.error("Error storing historical data for machine: {}", machineTelemetry.getMachineId(), e);
        }
    }

    private com.industrial.digitaltwin.alertanalytics.model.MachineStatus convertStatus(String statusStr) {
        if (statusStr == null) {
            return com.industrial.digitaltwin.alertanalytics.model.MachineStatus.NORMAL; // default
        }

        try {
            return com.industrial.digitaltwin.alertanalytics.model.MachineStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown status value: {}, defaulting to NORMAL", statusStr);
            return com.industrial.digitaltwin.alertanalytics.model.MachineStatus.NORMAL;
        }
    }
}