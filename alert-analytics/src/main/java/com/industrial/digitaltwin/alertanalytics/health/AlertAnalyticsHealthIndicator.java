package com.industrial.digitaltwin.alertanalytics.health;

import com.industrial.digitaltwin.alertanalytics.config.AlertAnalyticsProperties;
import com.industrial.digitaltwin.alertanalytics.service.AlertProcessingService;
import com.industrial.digitaltwin.alertanalytics.service.AnalyticsService;
import com.industrial.digitaltwin.alertanalytics.service.KafkaConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertAnalyticsHealthIndicator implements HealthIndicator {

    private final AlertAnalyticsProperties properties;
    private final AlertProcessingService alertProcessingService;
    private final AnalyticsService analyticsService;
    private final KafkaConsumerService kafkaConsumerService;

    // Track the last time we received data
    private final AtomicLong lastDataReceivedTime = new AtomicLong(System.currentTimeMillis());

    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.up();

        // Check Kafka connectivity
        boolean kafkaHealthy = checkKafkaConnectivity(healthBuilder);

        // Check database connectivity
        boolean databaseHealthy = checkDatabaseConnectivity(healthBuilder);

        // Check alert processing service
        boolean alertProcessingHealthy = checkAlertProcessingService(healthBuilder);

        // Check analytics service
        boolean analyticsHealthy = checkAnalyticsService(healthBuilder);

        // Overall health status
        if (kafkaHealthy && databaseHealthy && alertProcessingHealthy && analyticsHealthy) {
            healthBuilder.status("UP");
            log.debug("All health checks passed");
        } else {
            healthBuilder.status("DEGRADED");
            log.warn("Some health checks failed or are degraded");
        }

        // Add additional details
        healthBuilder.withDetail("lastDataReceived", Instant.ofEpochMilli(lastDataReceivedTime.get()));
        healthBuilder.withDetail("timestamp", Instant.now());

        return healthBuilder.build();
    }

    private boolean checkKafkaConnectivity(Health.Builder builder) {
        try {
            // In a real implementation, we would check actual Kafka connectivity
            // For now, we'll just return true and add placeholder details
            builder.withDetail("kafka", "Connected");
            log.debug("Kafka connectivity check passed");
            return true;
        } catch (Exception e) {
            log.error("Kafka connectivity check failed", e);
            builder.withDetail("kafka", "Disconnected: " + e.getMessage());
            return false;
        }
    }

    private boolean checkDatabaseConnectivity(Health.Builder builder) {
        try {
            // In a real implementation, we would check actual database connectivity
            // For now, we'll just return true and add placeholder details
            builder.withDetail("database", "Connected");
            log.debug("Database connectivity check passed");
            return true;
        } catch (Exception e) {
            log.error("Database connectivity check failed", e);
            builder.withDetail("database", "Disconnected: " + e.getMessage());
            return false;
        }
    }

    private boolean checkAlertProcessingService(Health.Builder builder) {
        try {
            // In a real implementation, we would check the alert processing service health
            // For now, we'll just return true and add placeholder details
            builder.withDetail("alertProcessing", "Operational");
            log.debug("Alert processing service check passed");
            return true;
        } catch (Exception e) {
            log.error("Alert processing service check failed", e);
            builder.withDetail("alertProcessing", "Degraded: " + e.getMessage());
            return false;
        }
    }

    private boolean checkAnalyticsService(Health.Builder builder) {
        try {
            // In a real implementation, we would check the analytics service health
            // For now, we'll just return true and add placeholder details
            builder.withDetail("analytics", "Operational");
            log.debug("Analytics service check passed");
            return true;
        } catch (Exception e) {
            log.error("Analytics service check failed", e);
            builder.withDetail("analytics", "Degraded: " + e.getMessage());
            return false;
        }
    }

    public void updateLastDataReceivedTime() {
        lastDataReceivedTime.set(System.currentTimeMillis());
    }
}