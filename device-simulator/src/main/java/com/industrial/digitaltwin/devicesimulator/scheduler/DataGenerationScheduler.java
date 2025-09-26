package com.industrial.digitaltwin.devicesimulator.scheduler;

import com.industrial.digitaltwin.devicesimulator.config.SimulationConfig;
import com.industrial.digitaltwin.devicesimulator.model.MachineTelemetry;
import com.industrial.digitaltwin.devicesimulator.service.MachineDataService;
import com.industrial.digitaltwin.devicesimulator.service.SimulatorMachineDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataGenerationScheduler {

    private final SimulationConfig simulationConfig;
    private final SimulatorMachineDataSource simulatorDataSource;
    private final MachineDataService machineDataService;

    /**
     * Scheduled method to generate and publish machine telemetry data
     * Runs based on the configured sampling interval
     */
    @Scheduled(fixedRateString = "${app.simulation.sampling-interval: 5000}")
    public void generateAndPublishData() {
        log.debug("Generating and publishing machine telemetry data...");
        
        try {
            // Generate telemetry for each configured machine
            List<SimulationConfig.MachineConfig> machines = simulatorDataSource.getMachineConfigs();
            
            for (SimulationConfig.MachineConfig config : machines) {
                // Generate telemetry data for this machine using the simulator data source
                MachineTelemetry telemetry = simulatorDataSource.generateTelemetry(config);
                
                // Publish the telemetry to Kafka
                machineDataService.publishTelemetry(telemetry);
                
                log.debug("Published telemetry for machine: {} at {}", 
                    telemetry.getMachineId(), telemetry.getTimestamp());
            }
        } catch (Exception e) {
            log.error("Error during data generation and publishing", e);
        }
    }
}