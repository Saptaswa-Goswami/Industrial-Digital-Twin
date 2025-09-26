package com.industrial.digitaltwin.devicesimulator.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.industrial.digitaltwin.devicesimulator.model.MachineTelemetry;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Primary
@Lazy
@Slf4j
public class MachineDataService {

    private final SimulatorMachineDataSource simulatorDataSource;
    
    @Autowired(required = false) // Optional - only if bean exists
    @Qualifier("opc-ua")
    private OpcUaMachineDataSource opcUaDataSource;
    
    @Autowired(required = false) // Optional - only if bean exists
    @Qualifier("mqtt")
    private MqttMachineDataSource mqttDataSource;
    
    private final KafkaProducerService kafkaProducerService;

    @Value("${app.data-source.mode: simulator}")
    private String dataSourceMode;

    private MachineDataSource activeDataSource;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public MachineDataService(SimulatorMachineDataSource simulatorDataSource, KafkaProducerService kafkaProducerService) {
        this.simulatorDataSource = simulatorDataSource;
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing Machine Data Service with mode: {}", dataSourceMode);
        
        // Select the appropriate data source based on configuration
        switch (dataSourceMode) {
            case "opc-ua":
                if (opcUaDataSource != null) {
                    activeDataSource = opcUaDataSource;
                    log.info("Using OPC-UA data source");
                } else {
                    log.warn("OPC-UA data source not available, falling back to simulator");
                    activeDataSource = simulatorDataSource;
                }
                break;
            case "mqtt":
                if (mqttDataSource != null) {
                    activeDataSource = mqttDataSource;
                    log.info("Using MQTT data source");
                } else {
                    log.warn("MQTT data source not available, falling back to simulator");
                    activeDataSource = simulatorDataSource;
                }
                break;
            case "simulator":
            default:
                activeDataSource = simulatorDataSource;
                log.info("Using simulator data source");
                break;
        }
        
        activeDataSource.initialize();
    }

    public void startDataGeneration() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("Starting data generation using {}", activeDataSource.getSourceType());
            
            // Start data generation with a callback that sends data to Kafka
            activeDataSource.startDataGeneration(() -> {
                // In a real implementation, this would be where we generate and send telemetry
                // For now, we'll just log that the callback was called
                log.debug("Data generation callback executed");
            });
        } else {
            log.warn("Data generation is already running");
        }
    }

    public void publishTelemetry(MachineTelemetry telemetry) {
        log.debug("Publishing telemetry for machine: {}", telemetry.getMachineId());
        kafkaProducerService.sendTelemetry(telemetry)
            .exceptionally(throwable -> {
                log.error("Failed to publish telemetry for machine: {}", telemetry.getMachineId(), throwable);
                return null;
            });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Machine Data Service");
        if (activeDataSource != null) {
            activeDataSource.shutdown();
        }
        isRunning.set(false);
    }

    public String getActiveDataSourceType() {
        return activeDataSource != null ? activeDataSource.getSourceType() : "unknown";
    }
}