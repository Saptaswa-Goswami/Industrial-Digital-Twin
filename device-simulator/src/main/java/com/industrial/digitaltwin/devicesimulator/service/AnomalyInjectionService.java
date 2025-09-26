package com.industrial.digitaltwin.devicesimulator.service;

import com.industrial.digitaltwin.devicesimulator.model.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class AnomalyInjectionService {

    private final Random random = new Random();

    public SensorData injectAnomaly(String machineType, SensorData sensorData) {
        log.debug("Injecting anomaly for machine type: {}", machineType);
        
        // Clone the sensor data and apply anomaly
        double newTemperature = sensorData.getTemperature();
        double newVibration = sensorData.getVibration();
        double newLoad = sensorData.getLoad();
        double newPressure = sensorData.getPressure();
        double newRpm = sensorData.getRpm();

        // Determine anomaly type based on machine type
        String anomalyType = determineAnomalyType(machineType);
        
        switch (anomalyType) {
            case "temperature":
                // Inject high temperature anomaly
                double tempIncrease = 15 + random.nextDouble() * 10; // 15-25 degree increase
                newTemperature = sensorData.getTemperature() + tempIncrease;
                log.debug("Injected temperature anomaly: {} -> {}", 
                    sensorData.getTemperature(), newTemperature);
                break;
                
            case "vibration":
                // Inject high vibration anomaly
                double vibrationIncrease = 2 + random.nextDouble() * 3; // 2-5 vibration increase
                newVibration = sensorData.getVibration() + vibrationIncrease;
                log.debug("Injected vibration anomaly: {} -> {}", 
                    sensorData.getVibration(), newVibration);
                break;
                
            case "load":
                // Inject overload anomaly
                double loadIncrease = 10 + random.nextDouble() * 15; // 10-25% load increase
                newLoad = Math.min(100, sensorData.getLoad() + loadIncrease);
                log.debug("Injected load anomaly: {} -> {}", 
                    sensorData.getLoad(), newLoad);
                break;
                
            case "pressure":
                // Inject pressure anomaly
                double pressureChange = (random.nextBoolean() ? 1 : -1) * (3 + random.nextDouble() * 4); // -7 to +7 pressure change
                newPressure = Math.max(1, sensorData.getPressure() + pressureChange);
                log.debug("Injected pressure anomaly: {} -> {}", 
                    sensorData.getPressure(), newPressure);
                break;
                
            case "rpm":
                // Inject RPM anomaly
                double rpmChange = (random.nextBoolean() ? 1 : -1) * (100 + random.nextDouble() * 200); // -300 to +300 RPM change
                newRpm = Math.max(0, sensorData.getRpm() + rpmChange);
                log.debug("Injected RPM anomaly: {} -> {}", 
                    sensorData.getRpm(), newRpm);
                break;
                
            default:
                // For other anomalies, apply random changes to multiple sensors
                newTemperature = sensorData.getTemperature() + (random.nextBoolean() ? 1 : -1) * (5 + random.nextDouble() * 10);
                newVibration = sensorData.getVibration() + (random.nextDouble() * 2);
                newLoad = Math.min(100, Math.max(0, sensorData.getLoad() + (random.nextBoolean() ? 1 : -1) * (10 + random.nextDouble() * 15)));
                break;
        }

        return SensorData.builder()
            .temperature(newTemperature)
            .vibration(newVibration)
            .load(newLoad)
            .pressure(newPressure)
            .rpm(newRpm)
            .build();
    }

    private String determineAnomalyType(String machineType) {
        // Different machine types are prone to different types of anomalies
        switch (machineType.toLowerCase()) {
            case "centrifugal-pump":
            case "cooling-pump":
                return "pressure"; // Pumps often have pressure-related issues
            case "rotary-compressor":
            case "reciprocating-compressor":
                return "temperature"; // Compressors prone to overheating
            case "industrial-motor":
                return "vibration"; // Motors prone to vibration issues
            case "conveyor-belt":
                return "load"; // Conveyors prone to overload
            case "steam-turbine":
                return "rpm"; // Turbines prone to RPM fluctuations
            case "hydraulic-press":
                return "pressure"; // Hydraulic systems prone to pressure issues
            default:
                // For other machine types, randomly select anomaly type
                String[] anomalyTypes = {"temperature", "vibration", "load", "pressure", "rpm"};
                return anomalyTypes[random.nextInt(anomalyTypes.length)];
        }
    }
}