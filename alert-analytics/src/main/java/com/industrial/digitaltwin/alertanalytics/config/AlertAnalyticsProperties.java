package com.industrial.digitaltwin.alertanalytics.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.alert-analytics")
@Validated
@Data
public class AlertAnalyticsProperties {

    @NestedConfigurationProperty
    private AlertProcessingProperties alertProcessing = new AlertProcessingProperties();

    @NestedConfigurationProperty
    private AnalyticsProperties analytics = new AnalyticsProperties();

    @NestedConfigurationProperty
    private NotificationProperties notification = new NotificationProperties();

    @Data
    public static class AlertProcessingProperties {
        @NotNull
        private Duration deduplicationWindow = Duration.ofMinutes(5); // 5 minutes for duplicate filtering
        @NotNull
        private Duration correlationWindow = Duration.ofMinutes(10);   // 10 minutes for related alert grouping
        @NotNull
        private Duration criticalThreshold = Duration.ofMinutes(2);     // Max time before escalation
    }

    @Data
    public static class AnalyticsProperties {
        @NotNull
        private Duration calculationInterval = Duration.ofMinutes(1);  // Calculate metrics every minute
        @NotNull
        private Duration reportingInterval = Duration.ofHours(1);    // Generate reports every hour
        @NotNull
        private Duration retentionPeriod = Duration.ofDays(30);      // Keep analytics data for 30 days
    }

    @Data
    public static class NotificationProperties {
        @NotNull
        private Duration criticalDelay = Duration.ofSeconds(30);       // Delay before escalating critical alerts
        private int retryAttempts = 3;         // Number of notification retry attempts
    }
}