package com.industrial.digitaltwin.devicesimulator.service;

import com.industrial.digitaltwin.devicesimulator.config.SimulationConfig;
import com.industrial.digitaltwin.devicesimulator.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component("simulator")
@ConditionalOnProperty(name = "app.data-source.mode", havingValue = "simulator", matchIfMissing = true)
@Slf4j
public class SimulatorMachineDataSource implements MachineDataSource {

    private final SimulationConfig simulationConfig;
    private final AnomalyInjectionService anomalyInjectionService;
    
    private ScheduledExecutorService scheduler;
    private List<SimulationConfig.MachineConfig> machineConfigs;
    private final Random random = new Random();

    public SimulatorMachineDataSource(SimulationConfig simulationConfig, AnomalyInjectionService anomalyInjectionService) {
        this.simulationConfig = simulationConfig;
        this.anomalyInjectionService = anomalyInjectionService;
    }

    @Override
    public void initialize() {
        log.info("Initializing Simulator Machine Data Source");
        this.scheduler = Executors.newScheduledThreadPool(simulationConfig.getMachines().size());
        this.machineConfigs = simulationConfig.getMachines();
        log.info("Initialized {} machines for simulation", machineConfigs.size());
    }

    @Override
    public void startDataGeneration(Runnable dataCallback) {
        log.info("Starting data generation for {} machines", machineConfigs.size());
        
        for (SimulationConfig.MachineConfig config : machineConfigs) {
            scheduleMachineDataGeneration(config, dataCallback);
        }
    }

    private void scheduleMachineDataGeneration(SimulationConfig.MachineConfig config, Runnable dataCallback) {
        long interval = simulationConfig.getSamplingInterval();
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                MachineTelemetry telemetry = generateTelemetry(config);
                
                // Execute the callback to process the telemetry
                dataCallback.run();
                
            } catch (Exception e) {
                log.error("Error generating telemetry for machine: {}", config.getId(), e);
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    public MachineTelemetry generateTelemetry(SimulationConfig.MachineConfig config) {
        // Generate base sensor values based on machine configuration
        double baseTemp = config.getBaselineTemp() + (random.nextGaussian() * config.getTempVariation() / 3);
        double baseVibration = config.getBaselineVibration() + (random.nextGaussian() * config.getVibrationVariation() / 3);
        double baseLoad = 70 + (random.nextDouble() * 25); // 70-95% load
        double basePressure = 8 + (random.nextDouble() * 4); // 8-12 pressure
        double baseRpm = 1400 + (random.nextInt(200) - 100); // 1300-1500 RPM

        // Apply operational pattern variations
        if ("intermittent".equalsIgnoreCase(config.getOperationalPattern())) {
            // Intermittent machines have periods of operation and rest
            if (random.nextDouble() > 0.7) { // 30% chance of being idle
                baseLoad = baseLoad * 0.1; // 10% of normal load when idle
            }
        }

        // Create sensor data
        SensorData sensorData = SensorData.builder()
                .temperature(Math.max(20, baseTemp)) // Ensure temp doesn't go below 20
                .vibration(Math.max(0.1, baseVibration)) // Ensure vibration doesn't go below 0.1
                .load(Math.min(100, Math.max(0, baseLoad))) // Keep load between 0-100%
                .pressure(Math.max(1, basePressure)) // Ensure pressure doesn't go below 1
                .rpm(Math.max(0, baseRpm)) // Ensure RPM doesn't go below 0
                .build();

        // Apply anomaly injection based on probability
        if (random.nextDouble() < simulationConfig.getAnomalyProbability()) {
            sensorData = anomalyInjectionService.injectAnomaly(config.getType(), sensorData);
        }

        // Determine machine status based on sensor values
        MachineStatus status = determineStatus(sensorData);

        // Build and return telemetry
        return MachineTelemetry.builder()
                .machineId(config.getId())
                .timestamp(Instant.now())
                .sensorData(sensorData)
                .status(status)
                .schemaVersion("1.0")
                .additionalMetrics(Map.of(
                        "operationalPattern", config.getOperationalPattern(),
                        "baselineTemp", config.getBaselineTemp(),
                        "baselineVibration", config.getBaselineVibration()
                ))
                .dataSourceType("simulator")
                .dataQualityScore(1.0)
                .build();
    }

    private MachineStatus determineStatus(SensorData sensorData) {
        // Check for OFFLINE status - if sensor data is unavailable or invalid
        if (sensorData == null) {
            return MachineStatus.OFFLINE;
        }
        
        // Check for ERROR status - critical failures
        if (sensorData.getTemperature() > 95 || sensorData.getVibration() > 6.0 || sensorData.getLoad() > 98) {
            return MachineStatus.ERROR;
        }
        
        // Check for CRITICAL status - approaching critical thresholds
        if (sensorData.getTemperature() > 88 || sensorData.getVibration() > 4.5 || sensorData.getLoad() > 92) {
            return MachineStatus.CRITICAL;
        }
        
        // Check for WARNING status - approaching normal thresholds
        if (sensorData.getTemperature() > 80 || sensorData.getVibration() > 3.5 || sensorData.getLoad() > 85) {
            return MachineStatus.WARNING;
        }
        
        // Check for MAINTENANCE status - if machine is scheduled for maintenance (would need additional logic)
        // For now, we'll implement based on sensor patterns that might indicate maintenance mode
        if (sensorData.getRpm() < 10 && sensorData.getLoad() < 5) {
            return MachineStatus.MAINTENANCE;
        }
        
        // Check for IDLE status - low activity
        if (sensorData.getLoad() < 15) {
            return MachineStatus.IDLE;
        }
        
        // Check for PEAK_LOAD status - high activity
        if (sensorData.getLoad() > 80) {
            return MachineStatus.PEAK_LOAD;
        }
        
        // Default to NORMAL status
        return MachineStatus.NORMAL;
    }

    public List<SimulationConfig.MachineConfig> getMachineConfigs() {
        return machineConfigs;
    }

    @Override
    public void shutdown() {
        log.info("Shutting down Simulator Machine Data Source");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public String getSourceType() {
        return "simulator";
    }
}