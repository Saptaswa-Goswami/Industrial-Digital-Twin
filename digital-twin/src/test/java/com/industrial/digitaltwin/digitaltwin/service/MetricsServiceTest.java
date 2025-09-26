package com.industrial.digitaltwin.digitaltwin.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetricsServiceTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private StateManagementService stateManagementService;

    @InjectMocks
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldInitializeMetrics() {
        // When
        metricsService.initializeMetrics();

        // Then - No exceptions thrown during initialization
        // Metrics initialization happens without errors
    }

    @Test
    void shouldIncrementProcessedEvents() {
        // Given
        long initialCount = metricsService.getProcessedEventsCount();

        // When
        metricsService.incrementProcessedEvents();

        // Then
        long finalCount = metricsService.getProcessedEventsCount();
        assertEquals(initialCount + 1, finalCount);
    }

    @Test
    void shouldIncrementDetectedAnomalies() {
        // Given
        long initialCount = metricsService.getDetectedAnomaliesCount();

        // When
        metricsService.incrementDetectedAnomalies();

        // Then
        long finalCount = metricsService.getDetectedAnomaliesCount();
        assertEquals(initialCount + 1, finalCount);
    }

    @Test
    void shouldIncrementStateUpdates() {
        // Given
        long initialCount = metricsService.getStateUpdatesCount();

        // When
        metricsService.incrementStateUpdates();

        // Then
        long finalCount = metricsService.getStateUpdatesCount();
        assertEquals(initialCount + 1, finalCount);
    }
}