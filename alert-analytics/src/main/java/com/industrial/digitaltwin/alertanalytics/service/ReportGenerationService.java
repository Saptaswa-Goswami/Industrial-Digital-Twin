package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.entity.MaintenanceReportEntity;
import com.industrial.digitaltwin.alertanalytics.model.MaintenanceReport;
import com.industrial.digitaltwin.alertanalytics.model.PerformanceMetrics;
import com.industrial.digitaltwin.alertanalytics.model.ReportType;
import com.industrial.digitaltwin.alertanalytics.repository.MaintenanceReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationService {

    private final MaintenanceReportRepository reportRepository;
    private final AnalyticsService analyticsService;

    public MaintenanceReport generateReport(ReportType reportType, String machineId) {
        log.info("Generating {} report for machine: {}", reportType, machineId);

        try {
            // Get performance metrics for the machine based on report type
            List<PerformanceMetrics> metricsList = getPerformanceMetricsForReport(reportType, machineId);

            // Generate report content based on metrics
            Map<String, Object> reportContent = generateReportContent(metricsList, reportType, machineId);
            Map<String, Object> metricsSummary = generateMetricsSummary(metricsList);
            List<String> recommendations = generateRecommendations(metricsList);

            // Create maintenance report
            MaintenanceReport report = MaintenanceReport.builder()
                    .reportId("REPORT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .reportType(reportType)
                    .generatedTime(Instant.now())
                    .machineId(machineId)
                    .content(reportContent)
                    .metricsSummary(metricsSummary)
                    .recommendations(recommendations)
                    .build();

            // Save to database
            MaintenanceReportEntity entity = convertToEntity(report);
            reportRepository.save(entity);

            log.info("Report generated and saved: {}", report.getReportId());
            return report;
        } catch (Exception e) {
            log.error("Error generating {} report for machine: {}", reportType, machineId, e);
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    private List<PerformanceMetrics> getPerformanceMetricsForReport(ReportType reportType, String machineId) {
        // Fetch metrics based on the report type and time period
        Instant startTime = getStartTimeForReportType(reportType);
        List<com.industrial.digitaltwin.alertanalytics.model.SensorData> historicalData =
            analyticsService.getHistoricalData(machineId);
        
        // Handle case where no historical data exists
        if (historicalData == null || historicalData.isEmpty()) {
            log.warn("No historical data found for machine: {}", machineId);
            // Return a default PerformanceMetrics object with default values
            PerformanceMetrics defaultMetrics = PerformanceMetrics.builder()
                .machineId(machineId)
                .averageLoad(0.0)
                .averageTemperature(0.0)
                .averageVibration(0.0)
                .uptimePercentage(0.0)
                .efficiencyRating(0.0)
                .anomalyCount(0)
                .build();
            return List.of(defaultMetrics);
        }
        
        // For now, we'll use all historical data since we don't have timestamps in SensorData
        // In a real implementation, we would need to associate timestamps with SensorData
        PerformanceMetrics metrics = analyticsService.calculatePerformanceMetrics(machineId, historicalData);
        return List.of(metrics);
    }

    private Instant getStartTimeForReportType(ReportType reportType) {
        Instant now = Instant.now();
        switch (reportType) {
            case DAILY:
                return now.minus(1, ChronoUnit.DAYS);
            case WEEKLY:
                return now.minus(7, ChronoUnit.DAYS);
            case MONTHLY:
                return now.minus(30, ChronoUnit.DAYS);
            case QUARTERLY:
                return now.minus(90, ChronoUnit.DAYS);
            case ANNUAL:
                return now.minus(365, ChronoUnit.DAYS);
            default:
                return now.minus(7, ChronoUnit.DAYS); // Default to weekly
        }
    }

    private Map<String, Object> generateReportContent(List<PerformanceMetrics> metricsList, ReportType reportType, String machineId) {
        // Generate detailed content based on report type and metrics
        return Map.of(
            "reportTitle", generateReportTitle(reportType, machineId),
            "reportType", reportType.name(),
            "machineId", machineId,
            "metricsCount", metricsList.size(),
            "period", getReportPeriod(reportType),
            "generatedAt", Instant.now(),
            "metrics", metricsList,
            "summary", generateMetricsSummary(metricsList)
        );
    }

    private String generateReportTitle(ReportType reportType, String machineId) {
        return String.format("%s Maintenance Report for Machine %s",
            reportType.name().charAt(0) + reportType.name().substring(1).toLowerCase(),
            machineId);
    }

    private String getReportPeriod(ReportType reportType) {
        switch (reportType) {
            case DAILY:
                return "24 hours";
            case WEEKLY:
                return "7 days";
            case MONTHLY:
                return "30 days";
            case QUARTERLY:
                return "90 days";
            case ANNUAL:
                return "365 days";
            default:
                return "Custom period";
        }
    }

    private Map<String, Object> generateMetricsSummary(List<PerformanceMetrics> metricsList) {
        if (metricsList.isEmpty()) {
            return Map.of();
        }

        // Calculate summary metrics
        double avgLoad = metricsList.stream().mapToDouble(PerformanceMetrics::getAverageLoad).average().orElse(0.0);
        double avgTemp = metricsList.stream().mapToDouble(PerformanceMetrics::getAverageTemperature).average().orElse(0.0);
        double avgVibration = metricsList.stream().mapToDouble(PerformanceMetrics::getAverageVibration).average().orElse(0.0);
        double avgUptime = metricsList.stream().mapToDouble(PerformanceMetrics::getUptimePercentage).average().orElse(0.0);
        double avgEfficiency = metricsList.stream().mapToDouble(PerformanceMetrics::getEfficiencyRating).average().orElse(0.0);
        int totalAnomalies = metricsList.stream().mapToInt(PerformanceMetrics::getAnomalyCount).sum();

        return Map.of(
            "averageLoad", avgLoad,
            "averageTemperature", avgTemp,
            "averageVibration", avgVibration,
            "averageUptimePercentage", avgUptime,
            "averageEfficiencyRating", avgEfficiency,
            "totalAnomalyCount", totalAnomalies,
            "metricsCount", metricsList.size()
        );
    }

    private List<String> generateRecommendations(List<PerformanceMetrics> metricsList) {
        // Generate recommendations based on metrics
        if (metricsList.isEmpty()) {
            return List.of("No data available for recommendations");
        }

        PerformanceMetrics latestMetrics = metricsList.get(0);
        List<String> recommendations = new java.util.ArrayList<>();

        // Add recommendations based on metrics
        if (latestMetrics.getAverageTemperature() > 80.0) {
            recommendations.add("High temperature detected. Recommend cooling system check.");
        }

        if (latestMetrics.getAverageVibration() > 2.5) {
            recommendations.add("Excessive vibration detected. Recommend mechanical inspection.");
        }

        if (latestMetrics.getUptimePercentage() < 90.0) {
            recommendations.add("Low uptime percentage. Investigate downtime causes.");
        }

        if (latestMetrics.getAnomalyCount() > 10) {
            recommendations.add("High anomaly count. Recommend detailed diagnostics.");
        }

        if (latestMetrics.getEfficiencyRating() < 70.0) {
            recommendations.add("Low efficiency rating. Recommend performance optimization.");
        }

        // Add default recommendation if no specific issues
        if (recommendations.isEmpty()) {
            recommendations.add("System operating within normal parameters. Regular maintenance recommended.");
        }

        return recommendations;
    }

    private MaintenanceReportEntity convertToEntity(MaintenanceReport report) {
        return MaintenanceReportEntity.builder()
                .reportId(report.getReportId())
                .reportType(report.getReportType())
                .generatedTime(report.getGeneratedTime())
                .machineId(report.getMachineId())
                .content(report.getContent())
                .metricsSummary(report.getMetricsSummary())
                .recommendations(report.getRecommendations().stream()
                    .collect(Collectors.toMap(r -> "rec_" + r.hashCode(), r -> (Object) r)))
                .createdAt(Instant.now())
                .build();
    }
    
    // Methods required by the controller
    public List<MaintenanceReport> getAllReports() {
        // For now, return an empty list or implement based on your requirements
        // In a real implementation, you would fetch all reports from the database
        return List.of();
    }
    
    public java.util.Optional<MaintenanceReport> getReportById(String reportId) {
        // For now, return empty optional or implement based on your requirements
        // In a real implementation, you would fetch a specific report from the database
        return java.util.Optional.empty();
    }
    
    public List<MaintenanceReport> getReportsByMachineId(String machineId) {
        // For now, return an empty list or implement based on your requirements
        // In a real implementation, you would fetch reports for a specific machine from the database
        return List.of();
    }
    
    public List<MaintenanceReport> getReportsByType(ReportType reportType) {
        // For now, return an empty list or implement based on your requirements
        // In a real implementation, you would fetch reports of a specific type from the database
        return List.of();
    }
    
    public MaintenanceReport generateReport(String machineId, ReportType reportType) {
        log.info("Generating {} report for machine: {}", reportType, machineId);

        try {
            // Get performance metrics for the machine based on report type
            List<PerformanceMetrics> metricsList = getPerformanceMetricsForReport(reportType, machineId);

            // Generate report content based on metrics
            Map<String, Object> reportContent = generateReportContent(metricsList, reportType, machineId);
            Map<String, Object> metricsSummary = generateMetricsSummary(metricsList);
            List<String> recommendations = generateRecommendations(metricsList);

            // Create maintenance report
            MaintenanceReport report = MaintenanceReport.builder()
                    .reportId("REPORT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .reportType(reportType)
                    .generatedTime(Instant.now())
                    .machineId(machineId)
                    .content(reportContent)
                    .metricsSummary(metricsSummary)
                    .recommendations(recommendations)
                    .build();

            // Save to database
            MaintenanceReportEntity entity = convertToEntity(report);
            reportRepository.save(entity);

            log.info("Report generated and saved: {}", report.getReportId());
            return report;
        } catch (Exception e) {
            log.error("Error generating {} report for machine: {}", reportType, machineId, e);
            throw new RuntimeException("Failed to generate report", e);
        }
    }
}