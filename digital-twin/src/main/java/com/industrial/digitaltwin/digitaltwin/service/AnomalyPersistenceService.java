package com.industrial.digitaltwin.digitaltwin.service;

import com.industrial.digitaltwin.digitaltwin.model.AlertSeverity;
import com.industrial.digitaltwin.digitaltwin.model.AnomalyEvent;
import com.industrial.digitaltwin.digitaltwin.model.AnomalyRecord;
import com.industrial.digitaltwin.digitaltwin.model.AnomalyType;
import com.industrial.digitaltwin.digitaltwin.repository.AnomalyHistory;
import com.industrial.digitaltwin.digitaltwin.repository.AnomalyHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AnomalyPersistenceService {

    @Autowired
    private AnomalyHistoryRepository anomalyHistoryRepository;

    @KafkaListener(topics = "anomaly-events", groupId = "digital-twin-persistence-group")
    public void persistAnomalyEvent(Map<String, Object> rawAnomalyEvent) {
        log.info("Received raw anomaly event: {}", rawAnomalyEvent);
        
        try {
            AnomalyEvent anomalyEvent = convertToAnomalyEvent(rawAnomalyEvent);
            log.info("Persisting anomaly event for machine: {} with {} anomalies",
                    anomalyEvent.getMachineId(), anomalyEvent.getAnomalies().size());
            
            for (AnomalyRecord anomaly : anomalyEvent.getAnomalies()) {
                try {
                    // Create anomaly history record
                    AnomalyHistory historyRecord = AnomalyHistory.builder()
                        .machineId(anomalyEvent.getMachineId())
                        .anomalyType(anomaly.getType())
                        .severity(anomaly.getSeverity())
                        .timestamp(anomalyEvent.getTimestamp())
                        .sensorData(createSensorDataMap(anomaly))
                        .calculatedMetrics(createCalculatedMetricsMap(anomaly))
                        .details(createDetailsMap(anomaly))
                        .createdAt(Instant.now())
                        .build();
                    
                    anomalyHistoryRepository.save(historyRecord);
                    log.debug("Persisted anomaly history for machine: {} type: {}",
                            anomaly.getMachineId(), anomaly.getType());
                    
                } catch (Exception e) {
                    log.error("Error persisting anomaly for machine: {}", anomaly.getMachineId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error processing anomaly event: {}", rawAnomalyEvent, e);
        }
    }
    
    private AnomalyEvent convertToAnomalyEvent(Map<String, Object> rawAnomalyEvent) {
        String machineId = (String) rawAnomalyEvent.get("machineId");
        String algorithmUsed = (String) rawAnomalyEvent.get("algorithmUsed");
        Object timestampObj = rawAnomalyEvent.get("timestamp");
        
        // Handle timestamp as either String (ISO format) or numeric (epoch seconds with decimals)
        Instant timestamp = null;
        if (timestampObj instanceof String) {
            timestamp = Instant.parse((String) timestampObj);
        } else if (timestampObj instanceof Number) {
            // Convert from epoch seconds with decimal to Instant
            double epochSeconds = ((Number) timestampObj).doubleValue();
            long seconds = (long) epochSeconds;
            long nanos = (long) ((epochSeconds - seconds) * 1_000_000_000);
            timestamp = Instant.ofEpochSecond(seconds, nanos);
        } else {
            timestamp = Instant.now();
        }
        
        // Convert anomalies list
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawAnomalies = (List<Map<String, Object>>) rawAnomalyEvent.get("anomalies");
        List<AnomalyRecord> anomalies = new ArrayList<>();
        
        if (rawAnomalies != null) {
            for (Map<String, Object> rawAnomaly : rawAnomalies) {
                AnomalyRecord anomaly = convertToAnomalyRecord(rawAnomaly);
                anomalies.add(anomaly);
            }
        }
        
        return AnomalyEvent.builder()
            .machineId(machineId)
            .anomalies(anomalies)
            .timestamp(timestamp)
            .algorithmUsed(algorithmUsed)
            .build();
    }
    
    private AnomalyRecord convertToAnomalyRecord(Map<String, Object> rawAnomaly) {
        String machineId = (String) rawAnomaly.get("machineId");
        String typeStr = (String) rawAnomaly.get("type");
        String severityStr = (String) rawAnomaly.get("severity");
        Object timestampObj = rawAnomaly.get("timestamp");
        String description = (String) rawAnomaly.get("description");
        Double value = (Double) rawAnomaly.get("value");
        Double threshold = (Double) rawAnomaly.get("threshold");
        
        // Handle timestamp as either String (ISO format) or numeric (epoch seconds with decimals)
        Instant timestamp = null;
        if (timestampObj instanceof String) {
            timestamp = Instant.parse((String) timestampObj);
        } else if (timestampObj instanceof Number) {
            // Convert from epoch seconds with decimal to Instant
            double epochSeconds = ((Number) timestampObj).doubleValue();
            long seconds = (long) epochSeconds;
            long nanos = (long) ((epochSeconds - seconds) * 1_000_000);
            timestamp = Instant.ofEpochSecond(seconds, nanos);
        } else {
            timestamp = Instant.now();
        }
        
        // Convert type and severity strings to enums
        AnomalyType type = convertAnomalyType(typeStr);
        AlertSeverity severity = convertAlertSeverity(severityStr);
        
        return AnomalyRecord.builder()
            .machineId(machineId)
            .type(type)
            .severity(severity)
            .timestamp(timestamp)
            .description(description)
            .value(value != null ? value : 0.0)
            .threshold(threshold != null ? threshold : 0.0)
            .build();
    }
    
    private AnomalyType convertAnomalyType(String typeStr) {
        if (typeStr == null) {
            return AnomalyType.TEMPERATURE_HIGH; // default
        }
        try {
            return AnomalyType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown anomaly type: {}, defaulting to TEMPERATURE_HIGH", typeStr);
            return AnomalyType.TEMPERATURE_HIGH;
        }
    }
    
    private AlertSeverity convertAlertSeverity(String severityStr) {
        if (severityStr == null) {
            return AlertSeverity.INFO; // default
        }
        try {
            return AlertSeverity.valueOf(severityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown alert severity: {}, defaulting to INFO", severityStr);
            return AlertSeverity.INFO;
        }
    }
    
    private Map<String, Object> createSensorDataMap(AnomalyRecord anomaly) {
        Map<String, Object> sensorData = new HashMap<>();
        sensorData.put("value", anomaly.getValue());
        sensorData.put("threshold", anomaly.getThreshold());
        return sensorData;
    }
    
    private Map<String, Object> createCalculatedMetricsMap(AnomalyRecord anomaly) {
        Map<String, Object> metrics = new HashMap<>();
        // In the future, this could include additional calculated metrics
        metrics.put("deviation_from_threshold", Math.abs(anomaly.getValue() - anomaly.getThreshold()));
        return metrics;
    }
    
    private Map<String, Object> createDetailsMap(AnomalyRecord anomaly) {
        Map<String, Object> details = new HashMap<>();
        details.put("description", anomaly.getDescription());
        details.put("algorithm_used", "threshold-based");
        return details;
    }
}