package com.industrial.digitaltwin.alertanalytics.controller;

import com.industrial.digitaltwin.alertanalytics.model.AlertEvent;
import com.industrial.digitaltwin.alertanalytics.model.AlertSeverity;
import com.industrial.digitaltwin.alertanalytics.model.AlertStatus;
import com.industrial.digitaltwin.alertanalytics.model.MaintenanceReport;
import com.industrial.digitaltwin.alertanalytics.model.PerformanceMetrics;
import com.industrial.digitaltwin.alertanalytics.model.ReportType;
import com.industrial.digitaltwin.alertanalytics.service.AlertProcessingService;
import com.industrial.digitaltwin.alertanalytics.service.AnalyticsService;
import com.industrial.digitaltwin.alertanalytics.service.ReportGenerationService;
import com.industrial.digitaltwin.alertanalytics.service.ReplayEngine;
import com.industrial.digitaltwin.alertanalytics.service.HistoricalStateService;
import com.industrial.digitaltwin.alertanalytics.model.MachineTelemetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AlertAnalyticsController {

    @Autowired
    private AlertProcessingService alertProcessingService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ReportGenerationService reportGenerationService;

    // Alert Management Endpoints
    @GetMapping("/alerts")
    public ResponseEntity<List<AlertEvent>> getAllAlerts(
            @RequestParam(required = false) String machineId,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) AlertStatus status) {
        List<AlertEvent> alerts = alertProcessingService.getAlerts(machineId, severity, status);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/{alertId}")
    public ResponseEntity<AlertEvent> getAlertById(@PathVariable String alertId) {
        Optional<AlertEvent> alert = alertProcessingService.getAlertById(alertId);
        return alert.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/alerts/machine/{machineId}")
    public ResponseEntity<List<AlertEvent>> getAlertsByMachineId(@PathVariable String machineId) {
        List<AlertEvent> alerts = alertProcessingService.getAlertsByMachineId(machineId);
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/alerts/acknowledge/{alertId}")
    public ResponseEntity<AlertEvent> acknowledgeAlert(@PathVariable String alertId) {
        Optional<AlertEvent> alert = alertProcessingService.acknowledgeAlert(alertId);
        return alert.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/alerts/resolve/{alertId}")
    public ResponseEntity<AlertEvent> resolveAlert(@PathVariable String alertId) {
        Optional<AlertEvent> alert = alertProcessingService.resolveAlert(alertId);
        return alert.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/alerts/{alertId}")
    public ResponseEntity<String> deleteAlert(@PathVariable String alertId) {
        boolean deleted = alertProcessingService.deleteAlert(alertId);
        if (deleted) {
            return ResponseEntity.ok("Alert with ID " + alertId + " has been successfully deleted");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Analytics Endpoints
    @GetMapping("/analytics/metrics")
    public ResponseEntity<List<PerformanceMetrics>> getAllPerformanceMetrics() {
        List<PerformanceMetrics> metrics = analyticsService.getAllPerformanceMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/analytics/metrics/{machineId}")
    public ResponseEntity<PerformanceMetrics> getPerformanceMetrics(@PathVariable String machineId) {
        Optional<PerformanceMetrics> metrics = analyticsService.getPerformanceMetrics(machineId);
        return metrics.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/analytics/metrics/{machineId}/historical")
    public ResponseEntity<List<PerformanceMetrics>> getHistoricalPerformanceMetrics(
            @PathVariable String machineId,
            @RequestParam(required = false) Long fromTime,
            @RequestParam(required = false) Long toTime) {
        List<PerformanceMetrics> metrics = analyticsService.getHistoricalPerformanceMetrics(
                machineId, fromTime, toTime);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/analytics/trends")
    public ResponseEntity<Object> getAllTrends() {
        Object trends = analyticsService.getAllTrends();
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/analytics/trends/{machineId}")
    public ResponseEntity<Object> getTrendsByMachineId(@PathVariable String machineId) {
        Object trends = analyticsService.getTrendsByMachineId(machineId);
        return ResponseEntity.ok(trends);
    }

    // Maintenance Reports Endpoints
    @GetMapping("/reports")
    public ResponseEntity<List<MaintenanceReport>> getAllReports() {
        List<MaintenanceReport> reports = reportGenerationService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<MaintenanceReport> getReportById(@PathVariable String reportId) {
        Optional<MaintenanceReport> report = reportGenerationService.getReportById(reportId);
        return report.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reports/machine/{machineId}")
    public ResponseEntity<List<MaintenanceReport>> getReportsByMachineId(@PathVariable String machineId) {
        List<MaintenanceReport> reports = reportGenerationService.getReportsByMachineId(machineId);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/reports/type/{reportType}")
    public ResponseEntity<List<MaintenanceReport>> getReportsByType(@PathVariable ReportType reportType) {
        List<MaintenanceReport> reports = reportGenerationService.getReportsByType(reportType);
        return ResponseEntity.ok(reports);
    }

    @PostMapping("/reports/generate")
    public ResponseEntity<MaintenanceReport> generateReport(
            @RequestParam(required = false) String machineId,
            @RequestParam ReportType reportType) {
        MaintenanceReport report = reportGenerationService.generateReport(machineId, reportType);
        return ResponseEntity.ok(report);
    }

    // Configuration endpoint
    @PostMapping("/config/reload")
    public ResponseEntity<Void> reloadConfig() {
        // This would trigger a configuration reload
        // For now, just return 200 OK
        return ResponseEntity.ok().build();
    }

    // Historical Replay Endpoints
    @PostMapping("/replay/{machineId}/start")
    public ResponseEntity<String> startReplay(
            @PathVariable String machineId,
            @RequestParam java.time.Instant startTime,
            @RequestParam java.time.Instant endTime,
            @RequestParam(defaultValue = "1.0") double speedMultiplier) {
        
        String replayId = replayEngine.startReplay(machineId, startTime, endTime, speedMultiplier);
        return ResponseEntity.ok(replayId);
    }

    @PostMapping("/replay/{replayId}/pause")
    public ResponseEntity<String> pauseReplay(@PathVariable String replayId) {
        boolean success = replayEngine.pauseReplay(replayId);
        if (success) {
            return ResponseEntity.ok("Replay paused successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/replay/{replayId}/resume")
    public ResponseEntity<String> resumeReplay(@PathVariable String replayId) {
        boolean success = replayEngine.resumeReplay(replayId);
        if (success) {
            return ResponseEntity.ok("Replay resumed successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/replay/{replayId}/stop")
    public ResponseEntity<String> stopReplay(@PathVariable String replayId) {
        boolean success = replayEngine.stopReplay(replayId);
        if (success) {
            return ResponseEntity.ok("Replay stopped successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/replay/{replayId}/seek")
    public ResponseEntity<String> seekReplay(
            @PathVariable String replayId,
            @RequestParam java.time.Instant targetTime) {
        
        boolean success = replayEngine.seekReplay(replayId, targetTime);
        if (success) {
            return ResponseEntity.ok("Replay seeked successfully to " + targetTime);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/replay/{replayId}/status")
    public ResponseEntity<ReplayEngine.ReplayStatus> getReplayStatus(@PathVariable String replayId) {
        ReplayEngine.ReplayStatus status = replayEngine.getReplayStatus(replayId);
        return status != null ? ResponseEntity.ok(status) : ResponseEntity.notFound().build();
    }

    @GetMapping("/history/{machineId}/telemetry")
    public ResponseEntity<List<MachineTelemetry>> getHistoricalTelemetry(
            @PathVariable String machineId,
            @RequestParam java.time.Instant fromTime,
            @RequestParam java.time.Instant toTime,
            @RequestParam(required = false, defaultValue = "60") int intervalSeconds) {
        
        List<MachineTelemetry> timeline = historicalStateService.getHistoricalStateTimeline(machineId, fromTime, toTime);
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/history/{machineId}/state")
    public ResponseEntity<MachineTelemetry> getHistoricalStateAtTime(
            @PathVariable String machineId,
            @RequestParam java.time.Instant timestamp) {
        
        MachineTelemetry state = historicalStateService.getHistoricalStateAtTime(machineId, timestamp);
        return ResponseEntity.ok(state);
    }
    
    @GetMapping("/replay/{machineId}/time-boundaries")
    public ResponseEntity<ReplayEngine.TimeBoundaries> getTimeBoundaries(@PathVariable String machineId) {
        java.util.Optional<ReplayEngine.TimeBoundaries> boundaries = replayEngine.getTimeBoundaries(machineId);
        return boundaries.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    // Autowire the new services
    @Autowired
    private ReplayEngine replayEngine;

    @Autowired
    private HistoricalStateService historicalStateService;
}