package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.model.AlertAnalyticsUpdate;
import com.industrial.digitaltwin.alertanalytics.model.AlertEvent;
import com.industrial.digitaltwin.alertanalytics.model.MachineTelemetry;
import com.industrial.digitaltwin.alertanalytics.model.MaintenanceReport;
import com.industrial.digitaltwin.alertanalytics.model.PerformanceMetrics;
import com.industrial.digitaltwin.alertanalytics.websocket.AlertAnalyticsWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebSocketBroadcastService {

    @Autowired
    private AlertAnalyticsWebSocketHandler webSocketHandler;

    public void broadcastAlertUpdate(Object alert) {
        try {
            String machineId = "UNKNOWN";
            String severity = "WARNING";
            
            if (alert instanceof AlertEvent) {
                AlertEvent alertEvent = (AlertEvent) alert;
                machineId = alertEvent.getMachineId();
                severity = alertEvent.getSeverity() != null ? alertEvent.getSeverity().toString() : "WARNING";
            }

            AlertAnalyticsUpdate update = AlertAnalyticsUpdate.builder()
                    .updateType("ALERT")
                    .timestamp(java.time.Instant.now())
                    .payload(alert)
                    .severity(severity)
                    .machineId(machineId)
                    .build();

            webSocketHandler.broadcastMessage(update);
            log.debug("Broadcasted alert update via WebSocket");
        } catch (Exception e) {
            log.error("Error broadcasting alert update via WebSocket", e);
        }
    }

    public void broadcastAnalyticsUpdate(Object analytics) {
        try {
            String machineId = "UNKNOWN";
            String severity = "INFO"; // Default severity
            
            if (analytics instanceof PerformanceMetrics) {
                PerformanceMetrics metrics = (PerformanceMetrics) analytics;
                machineId = metrics.getMachineId();
                
                // Set severity based on anomaly count - higher anomaly count indicates higher severity
                if (metrics.getAnomalyCount() > 10) {
                    severity = "CRITICAL";
                } else if (metrics.getAnomalyCount() > 5) {
                    severity = "WARNING";
                }
            }
            
            AlertAnalyticsUpdate update = AlertAnalyticsUpdate.builder()
                    .updateType("ANALYTICS")
                    .timestamp(java.time.Instant.now())
                    .payload(analytics)
                    .severity(severity)
                    .machineId(machineId)
                    .build();
            
            webSocketHandler.broadcastMessage(update);
            log.debug("Broadcasted analytics update via WebSocket");
        } catch (Exception e) {
            log.error("Error broadcasting analytics update via WebSocket", e);
        }
    }

    public void broadcastHistoricalState(Object state) {
        try {
            String machineId = "UNKNOWN";
            String severity = "INFO"; // Default severity
            
            if (state instanceof MachineTelemetry) {
                MachineTelemetry telemetry = (MachineTelemetry) state;
                machineId = telemetry.getMachineId();
                // Extract severity from the status field if available
                if (telemetry.getStatus() != null) {
                    severity = telemetry.getStatus().name(); // Convert MachineStatus enum to string
                }
            }
            
            AlertAnalyticsUpdate update = AlertAnalyticsUpdate.builder()
                    .updateType("HISTORICAL_STATE")
                    .timestamp(java.time.Instant.now())
                    .payload(state)
                    .severity(severity)
                    .machineId(machineId)
                    .build();
            
            webSocketHandler.broadcastMessage(update);
            log.debug("Broadcasted historical state via WebSocket");
        } catch (Exception e) {
            log.error("Error broadcasting historical state via WebSocket", e);
        }
    }

    public void broadcastReportUpdate(Object report) {
        try {
            String machineId = "UNKNOWN";
            String severity = "INFO"; // Default severity for reports
            
            if (report instanceof MaintenanceReport) {
                MaintenanceReport maintenanceReport = (MaintenanceReport) report;
                machineId = maintenanceReport.getMachineId();
                
                // Determine severity based on report content or type
                if (maintenanceReport.getReportType() != null) {
                    if (maintenanceReport.getReportType().name().contains("CRITICAL")) {
                        severity = "CRITICAL";
                    } else if (maintenanceReport.getReportType().name().contains("WARNING")) {
                        severity = "WARNING";
                    }
                }
                
                // If recommendations contain urgent items, increase severity
                if (maintenanceReport.getRecommendations() != null) {
                    for (String recommendation : maintenanceReport.getRecommendations()) {
                        if (recommendation != null &&
                            (recommendation.toUpperCase().contains("IMMEDIATE") ||
                             recommendation.toUpperCase().contains("URGENT") ||
                             recommendation.toUpperCase().contains("CRITICAL"))) {
                            severity = "CRITICAL";
                            break;
                        } else if (recommendation != null &&
                                   (recommendation.toUpperCase().contains("SOON") ||
                                    recommendation.toUpperCase().contains("WARNING"))) {
                            severity = "WARNING";
                        }
                    }
                }
            }
            
            AlertAnalyticsUpdate update = AlertAnalyticsUpdate.builder()
                    .updateType("REPORT")
                    .timestamp(java.time.Instant.now())
                    .payload(report)
                    .severity(severity)
                    .machineId(machineId)
                    .build();
            
            webSocketHandler.broadcastMessage(update);
            log.debug("Broadcasted report update via WebSocket");
        } catch (Exception e) {
            log.error("Error broadcasting report update via WebSocket", e);
        }
    }
}