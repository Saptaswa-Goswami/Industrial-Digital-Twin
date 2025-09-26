package com.industrial.digitaltwin.alertanalytics.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // Counters for various events
    private final Counter alertProcessedCounter;
    private final Counter anomalyDetectedCounter;
    private final Counter notificationSentCounter;
    private final Counter reportGeneratedCounter;
    private final Counter stateUpdatesCounter;

    // Timers for measuring performance
    private final Timer alertProcessingTimer;
    private final Timer analyticsCalculationTimer;
    private final Timer notificationDeliveryTimer;

    // Gauges for current state
    private final AtomicInteger activeAlertsGauge;
    private final AtomicInteger pendingNotificationsGauge;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.alertProcessedCounter = Counter.builder("alertanalytics.alerts.processed")
                .description("Total number of alerts processed")
                .register(meterRegistry);

        this.anomalyDetectedCounter = Counter.builder("alertanalytics.anomalies.detected")
                .description("Total number of anomalies detected")
                .register(meterRegistry);

        this.notificationSentCounter = Counter.builder("alertanalytics.notifications.sent")
                .description("Total number of notifications sent")
                .register(meterRegistry);

        this.reportGeneratedCounter = Counter.builder("alertanalytics.reports.generated")
                .description("Total number of reports generated")
                .register(meterRegistry);

        this.stateUpdatesCounter = Counter.builder("alertanalytics.state.updates")
                .description("Total number of state updates processed")
                .register(meterRegistry);

        // Initialize timers
        this.alertProcessingTimer = Timer.builder("alertanalytics.alerts.processing.time")
                .description("Time taken to process alerts")
                .register(meterRegistry);

        this.analyticsCalculationTimer = Timer.builder("alertanalytics.analytics.calculation.time")
                .description("Time taken to calculate analytics")
                .register(meterRegistry);

        this.notificationDeliveryTimer = Timer.builder("alertanalytics.notifications.delivery.time")
                .description("Time taken to deliver notifications")
                .register(meterRegistry);

        // Initialize gauges
        this.activeAlertsGauge = new AtomicInteger(0);
        this.pendingNotificationsGauge = new AtomicInteger(0);

        meterRegistry.gauge("alertanalytics.alerts.active", activeAlertsGauge);
        meterRegistry.gauge("alertanalytics.notifications.pending", pendingNotificationsGauge);
    }

    // Counter increment methods
    public void incrementAlertsProcessed() {
        alertProcessedCounter.increment();
        log.debug("Incremented alerts processed counter");
    }

    public void incrementAnomaliesDetected() {
        anomalyDetectedCounter.increment();
        log.debug("Incremented anomalies detected counter");
    }

    public void incrementNotificationsSent() {
        notificationSentCounter.increment();
        log.debug("Incremented notifications sent counter");
    }

    public void incrementReportsGenerated() {
        reportGeneratedCounter.increment();
        log.debug("Incremented reports generated counter");
    }

    public void incrementStateUpdates() {
        stateUpdatesCounter.increment();
        log.debug("Incremented state updates counter");
    }

    public void incrementAnalyticsCalculations() {
        // Using the anomaly detected counter as a proxy for analytics calculations
        anomalyDetectedCounter.increment();
        log.debug("Incremented analytics calculations counter");
    }

    // Timer recording methods
    public void recordAlertProcessingTime(long millis) {
        alertProcessingTimer.record(millis, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug("Recorded alert processing time: {} ms", millis);
    }

    public void recordAnalyticsCalculationTime(long millis) {
        analyticsCalculationTimer.record(millis, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug("Recorded analytics calculation time: {} ms", millis);
    }

    public void recordNotificationDeliveryTime(long millis) {
        notificationDeliveryTimer.record(millis, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug("Recorded notification delivery time: {} ms", millis);
    }

    // Gauge update methods
    public void setActiveAlerts(int count) {
        activeAlertsGauge.set(count);
        log.debug("Updated active alerts gauge: {}", count);
    }

    public void setPendingNotifications(int count) {
        pendingNotificationsGauge.set(count);
        log.debug("Updated pending notifications gauge: {}", count);
    }

    // Convenience methods for timing operations
    public <T> T timeAlertProcessing(java.util.function.Supplier<T> operation) {
        return alertProcessingTimer.record(operation);
    }

    public <T> T timeAnalyticsCalculation(java.util.function.Supplier<T> operation) {
        return analyticsCalculationTimer.record(operation);
    }

    public <T> T timeNotificationDelivery(java.util.function.Supplier<T> operation) {
        return notificationDeliveryTimer.record(operation);
    }
}