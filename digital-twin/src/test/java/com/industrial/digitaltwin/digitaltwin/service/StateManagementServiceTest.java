package com.industrial.digitaltwin.digitaltwin.service;

import com.industrial.digitaltwin.digitaltwin.config.StateStoreConfig;
import com.industrial.digitaltwin.digitaltwin.model.DigitalTwinState;
import com.industrial.digitaltwin.digitaltwin.model.MachineTelemetry;
import com.industrial.digitaltwin.digitaltwin.model.SensorData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StateManagementServiceTest {

    @Mock
    private StateStoreConfig stateStoreConfig;

    @InjectMocks
    private StateManagementService stateManagementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(stateStoreConfig.getStateStore().getMaxEntries()).thenReturn(1000);
    }

    @Test
    void shouldCreateInitialStateWhenMachineDoesNotExist() {
        // Given
        String machineId = "TEST_MACHINE_001";
        MachineTelemetry telemetry = createTestTelemetry(machineId);

        // When
        DigitalTwinState result = stateManagementService.updateState(machineId, telemetry);

        // Then
        assertNotNull(result);
        assertEquals(machineId, result.getMachineId());
        assertEquals(telemetry.getSensorData(), result.getCurrentSensorData());
    }

    @Test
    void shouldUpdateExistingState() {
        // Given
        String machineId = "TEST_MACHINE_001";
        MachineTelemetry initialTelemetry = createTestTelemetry(machineId);
        MachineTelemetry updatedTelemetry = MachineTelemetry.builder()
                .machineId(machineId)
                .timestamp(Instant.now())
                .sensorData(SensorData.builder()
                        .temperature(80.0)
                        .vibration(2.0)
                        .load(85.0)
                        .pressure(9.0)
                        .rpm(1600)
                        .build())
                .build();

        // When - First update
        DigitalTwinState firstResult = stateManagementService.updateState(machineId, initialTelemetry);
        // When - Second update
        DigitalTwinState secondResult = stateManagementService.updateState(machineId, updatedTelemetry);

        // Then
        assertNotNull(secondResult);
        assertEquals(machineId, secondResult.getMachineId());
        assertEquals(updatedTelemetry.getSensorData(), secondResult.getCurrentSensorData());
    }

    @Test
    void shouldReturnNullWhenStateDoesNotExist() {
        // Given
        String machineId = "NON_EXISTENT_MACHINE";

        // When
        DigitalTwinState result = stateManagementService.getState(machineId);

        // Then
        assertNull(result);
    }

    private MachineTelemetry createTestTelemetry(String machineId) {
        return MachineTelemetry.builder()
                .machineId(machineId)
                .timestamp(Instant.now())
                .sensorData(SensorData.builder()
                        .temperature(75.0)
                        .vibration(1.5)
                        .load(80.0)
                        .pressure(8.5)
                        .rpm(1500)
                        .build())
                .build();
    }
}