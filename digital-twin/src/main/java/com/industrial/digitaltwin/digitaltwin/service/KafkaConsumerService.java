package com.industrial.digitaltwin.digitaltwin.service;

import com.industrial.digitaltwin.digitaltwin.model.AnomalyEvent;
import com.industrial.digitaltwin.digitaltwin.model.DigitalTwinState;
import com.industrial.digitaltwin.digitaltwin.model.MachineStatus;
import com.industrial.digitaltwin.digitaltwin.model.MachineTelemetry;
import com.industrial.digitaltwin.digitaltwin.model.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
public class KafkaConsumerService {

    @Autowired
    private StateManagementService stateManagementService;
    
    @Autowired
    private AnomalyDetectionService anomalyDetectionService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private WebSocketBroadcastService webSocketBroadcastService;
    
    @Autowired
    private MetricsService metricsService;

    @KafkaListener(topics = "machine-telemetry", groupId = "digital-twin-group")
    public void consumeTelemetry(Map<String, Object> rawTelemetry) {
        log.info("Received raw telemetry: {}", rawTelemetry);
        
        try {
            // Convert the raw telemetry map to our internal MachineTelemetry model
            MachineTelemetry telemetry = convertToMachineTelemetry(rawTelemetry);
            
            log.info("Received telemetry for machine: {} at {}",
                    telemetry.getMachineId(), telemetry.getTimestamp());
            
            // Update the digital twin state
            DigitalTwinState updatedState = stateManagementService.updateState(
                telemetry.getMachineId(), telemetry);
            
            // Detect anomalies
            AnomalyEvent anomalyEvent = anomalyDetectionService.detectAnomalies(updatedState, telemetry);
            
            // Publish state update
            kafkaTemplate.send("machine-state-updates", updatedState.getMachineId(), updatedState);
            metricsService.incrementStateUpdates();
            
            // Publish anomaly event if detected
            if (anomalyEvent != null) {
                kafkaTemplate.send("anomaly-events", anomalyEvent.getMachineId(), anomalyEvent);
                log.warn("Anomaly detected for machine {}: {}",
                        anomalyEvent.getMachineId(), anomalyEvent.getAnomalies());
                metricsService.incrementDetectedAnomalies();
            }
            
            // Send WebSocket update for real-time monitoring
            try {
                webSocketBroadcastService.broadcastStateUpdate(updatedState);
            } catch (Exception e) {
                log.error("Error sending WebSocket update", e);
            }
            
            // Increment metrics
            metricsService.incrementProcessedEvents();
            
        } catch (Exception e) {
            log.error("Error processing telemetry: {}", rawTelemetry, e);
            // In a production system, we might want to send this to a dead letter queue
        }
    }
    
    private MachineTelemetry convertToMachineTelemetry(Map<String, Object> rawTelemetry) {
        String machineId = (String) rawTelemetry.get("machineId");
        Object timestampObj = rawTelemetry.get("timestamp");
        Map<String, Object> sensorDataMap = (Map<String, Object>) rawTelemetry.get("sensorData");
        String statusStr = (String) rawTelemetry.get("status");
        
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
        
        // Convert status from device simulator to digital twin status
        MachineStatus status = convertStatus(statusStr);
        
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
    
    private MachineStatus convertStatus(String statusStr) {
        if (statusStr == null) {
            return MachineStatus.NORMAL; // default
        }
        
        try {
            return MachineStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown status value: {}, defaulting to NORMAL", statusStr);
            return MachineStatus.NORMAL;
        }
    }
}