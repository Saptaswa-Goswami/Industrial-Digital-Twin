package com.industrial.digitaltwin.digitaltwin.service;

import com.industrial.digitaltwin.digitaltwin.config.StateStoreConfig;
import com.industrial.digitaltwin.digitaltwin.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnomalyDetectionServiceTest {

    @Mock
    private StateStoreConfig config;

    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(config.getAnomalyDetection().getTemperatureThreshold()).thenReturn(85.0);
        when(config.getAnomalyDetection().getVibrationThreshold()).thenReturn(3.0);
        when(config.getAnomalyDetection().getLoadThreshold()).thenReturn(95.0);
        when(config.getAnomalyDetection().getStatisticalDeviationMultiplier()).thenReturn(2.5);
    }

    @Test
    void shouldDetectTemperatureAnomaly() {
        // Given
        DigitalTwinState state = createTestState("TEST_MACHINE_001");
        MachineTelemetry telemetry = MachineTelemetry.builder()
                .machineId("TEST_MACHINE_001")
                .timestamp(Instant.now())
                .sensorData(SensorData.builder()
                        .temperature(90.0) // Above threshold
                        .vibration(1.5)
                        .load(80.0)
                        .pressure(8.5)
                        .rpm(1500)
                        .build())
                .build();

        // When
        AnomalyEvent result = anomalyDetectionService.detectAnomalies(state, telemetry);

        // Then
        assertNotNull(result);
        assertEquals("TEST_MACHINE_001", result.getMachineId());
        assertEquals(1, result.getAnomalies().size());
        assertEquals(AnomalyType.TEMPERATURE_HIGH, result.getAnomalies().get(0).getType());
        assertEquals(AlertSeverity.CRITICAL, result.getAnomalies().get(0).getSeverity());
    }

    @Test
    void shouldNotDetectAnomalyWhenWithinThreshold() {
        // Given
        DigitalTwinState state = createTestState("TEST_MACHINE_001");
        MachineTelemetry telemetry = MachineTelemetry.builder()
                .machineId("TEST_MACHINE_001")
                .timestamp(Instant.now())
                .sensorData(SensorData.builder()
                        .temperature(75.0) // Within threshold
                        .vibration(1.5)
                        .load(80.0)
                        .pressure(8.5)
                        .rpm(1500)
                        .build())
                .build();

        // When
        AnomalyEvent result = anomalyDetectionService.detectAnomalies(state, telemetry);

        // Then
        assertNull(result);
    }

    @Test
    void shouldDetectMultipleAnomalies() {
        // Given
        DigitalTwinState state = createTestState("TEST_MACHINE_001");
        MachineTelemetry telemetry = MachineTelemetry.builder()
                .machineId("TEST_MACHINE_001")
                .timestamp(Instant.now())
                .sensorData(SensorData.builder()
                        .temperature(90.0) // Above threshold
                        .vibration(4.0) // Above threshold
                        .load(80.0)
                        .pressure(8.5)
                        .rpm(1500)
                        .build())
                .build();

        // When
        AnomalyEvent result = anomalyDetectionService.detectAnomalies(state, telemetry);

        // Then
        assertNotNull(result);
        assertEquals("TEST_MACHINE_001", result.getMachineId());
        assertTrue(result.getAnomalies().size() >= 1); // At least one anomaly detected
    }

    private DigitalTwinState createTestState(String machineId) {
        return DigitalTwinState.builder()
                .machineId(machineId)
                .lastUpdated(Instant.now())
                .currentSensorData(SensorData.builder()
                        .temperature(70.0)
                        .vibration(1.0)
                        .load(75.0)
                        .build())
                .historicalData(List.of()) // Empty for now
                .build();
    }
}