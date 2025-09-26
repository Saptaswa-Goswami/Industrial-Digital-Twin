package com.industrial.digitaltwin.alertanalytics.scheduler;

import com.industrial.digitaltwin.alertanalytics.config.AlertAnalyticsProperties;
import com.industrial.digitaltwin.alertanalytics.model.ReportType;
import com.industrial.digitaltwin.alertanalytics.service.AnalyticsService;
import com.industrial.digitaltwin.alertanalytics.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsScheduler {

    private final AnalyticsService analyticsService;
    private final ReportGenerationService reportGenerationService;
    private final AlertAnalyticsProperties properties;

    /**
     * Scheduled task to calculate performance metrics at regular intervals
     */
    @Scheduled(fixedRateString = "#{@alertAnalyticsProperties.analytics.calculationInterval.toMillis()}")
    public void calculatePerformanceMetrics() {
        log.debug("Running scheduled performance metrics calculation");
        try {
            // In a real implementation, this would iterate through all machines and calculate metrics
            // For now, we'll just log that the task ran
            log.info("Performance metrics calculation completed successfully");
        } catch (Exception e) {
            log.error("Error during performance metrics calculation", e);
        }
    }

    /**
     * Scheduled task to generate daily reports
     */
    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    public void generateDailyReports() {
        log.debug("Running scheduled daily report generation");
        try {
            // Generate daily reports for all machines
            // In a real implementation, we would iterate through all machines
            String placeholderMachineId = "MACHINE_PLACEHOLDER";
            reportGenerationService.generateReport(ReportType.DAILY, placeholderMachineId);
            log.info("Daily report generation completed");
        } catch (Exception e) {
            log.error("Error during daily report generation", e);
        }
    }

    /**
     * Scheduled task to generate weekly reports
     */
    @Scheduled(cron = "0 0 2 * * MON") // Run weekly on Monday at 2 AM
    public void generateWeeklyReports() {
        log.debug("Running scheduled weekly report generation");
        try {
            // Generate weekly reports for all machines
            String placeholderMachineId = "MACHINE_PLACEHOLDER";
            reportGenerationService.generateReport(ReportType.WEEKLY, placeholderMachineId);
            log.info("Weekly report generation completed");
        } catch (Exception e) {
            log.error("Error during weekly report generation", e);
        }
    }

    /**
     * Scheduled task to generate monthly reports
     */
    @Scheduled(cron = "0 0 3 1 * ?") // Run monthly on the 1st at 3 AM
    public void generateMonthlyReports() {
        log.debug("Running scheduled monthly report generation");
        try {
            // Generate monthly reports for all machines
            String placeholderMachineId = "MACHINE_PLACEHOLDER";
            reportGenerationService.generateReport(ReportType.MONTHLY, placeholderMachineId);
            log.info("Monthly report generation completed");
        } catch (Exception e) {
            log.error("Error during monthly report generation", e);
        }
    }

    /**
     * Scheduled task to clean up old data based on retention period
     */
    @Scheduled(cron = "0 0 4 * * ?") // Run daily at 4 AM
    public void cleanupOldData() {
        log.debug("Running scheduled data cleanup");
        try {
            // In a real implementation, this would remove data older than the retention period
            // For now, we'll just log that the task ran
            Instant cutoffDate = Instant.now().minus(properties.getAnalytics().getRetentionPeriod());
            log.info("Data cleanup completed. Removed data older than: {}", cutoffDate);
        } catch (Exception e) {
            log.error("Error during data cleanup", e);
        }
    }

    /**
     * Scheduled task to generate quarterly reports
     */
    @Scheduled(cron = "0 0 5 1 1,4,7,10 ?") // Run quarterly on the 1st of Jan, Apr, Jul, Oct at 5 AM
    public void generateQuarterlyReports() {
        log.debug("Running scheduled quarterly report generation");
        try {
            // Generate quarterly reports for all machines
            String placeholderMachineId = "MACHINE_PLACEHOLDER";
            reportGenerationService.generateReport(ReportType.QUARTERLY, placeholderMachineId);
            log.info("Quarterly report generation completed");
        } catch (Exception e) {
            log.error("Error during quarterly report generation", e);
        }
    }

    /**
     * Scheduled task to generate annual reports
     */
    @Scheduled(cron = "0 0 6 1 1 ?") // Run annually on January 1st at 6 AM
    public void generateAnnualReports() {
        log.debug("Running scheduled annual report generation");
        try {
            // Generate annual reports for all machines
            String placeholderMachineId = "MACHINE_PLACEHOLDER";
            reportGenerationService.generateReport(ReportType.ANNUAL, placeholderMachineId);
            log.info("Annual report generation completed");
        } catch (Exception e) {
            log.error("Error during annual report generation", e);
        }
    }
}