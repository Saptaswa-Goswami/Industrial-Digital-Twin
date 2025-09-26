package com.industrial.digitaltwin.devicesimulator.service;

import com.industrial.digitaltwin.devicesimulator.model.SensorData;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AnomalyInjectionServiceTest {

    private final AnomalyInjectionService anomalyInjectionService = new AnomalyInjectionService();

    @Test
    void shouldInjectTemperatureAnomaly() {
        // Given
        SensorData originalData = SensorData.builder()
                .temperature(70.0)
                .vibration(1.5)
                .load(80.0)
                .pressure(8.5)
                .rpm(1500)
                .build();

        // When
        SensorData result = anomalyInjectionService.injectAnomaly("rotary-compressor", originalData);

        // Then
        assertTrue(result.getTemperature() > originalData.getTemperature(), 
            "Temperature should be increased for temperature anomaly");
        assertEquals(originalData.getVibration(), result.getVibration(), 
            "Vibration should remain unchanged for temperature anomaly");
    }

    @Test
    void shouldInjectVibrationAnomaly() {
        // Given
        SensorData originalData = SensorData.builder()
                .temperature(70.0)
                .vibration(1.5)
                .load(80.0)
                .pressure(8.5)
                .rpm(1500)
                .build();

        // When
        SensorData result = anomalyInjectionService.injectAnomaly("industrial-motor", originalData);

        // Then
        assertTrue(result.getVibration() > originalData.getVibration(), 
            "Vibration should be increased for vibration anomaly");
        assertEquals(originalData.getTemperature(), result.getTemperature(), 
            "Temperature should remain unchanged for vibration anomaly");
    }

    @Test
    void shouldInjectPressureAnomaly() {
        // Given
        SensorData originalData = SensorData.builder()
                .temperature(70.0)
                .vibration(1.5)
                .load(80.0)
                .pressure(8.5)
                .rpm(1500)
                .build();

        // When
        SensorData result = anomalyInjectionService.injectAnomaly("centrifugal-pump", originalData);

        // Then
        assertTrue(result.getPressure() != originalData.getPressure(), 
            "Pressure should be changed for pressure anomaly");
    }
}