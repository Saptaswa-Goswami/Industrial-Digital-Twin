package com.industrial.digitaltwin.devicesimulator.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component("mqtt")
@ConditionalOnProperty(name = "app.data-source.mode", havingValue = "mqtt")
@Slf4j
public class MqttMachineDataSource implements MachineDataSource {

    @Override
    public void startDataGeneration(Runnable dataCallback) {
        log.warn("MQTT data source is not yet implemented. Using simulator instead.");
        // Future implementation for real MQTT machines
    }

    @Override
    public void initialize() {
        log.info("Initializing MQTT Machine Data Source");
        // Initialize MQTT client connections
    }

    @Override
    public void shutdown() {
        log.info("Shutting down MQTT Machine Data Source");
        // Cleanup MQTT connections
    }

    @Override
    public String getSourceType() {
        return "mqtt";
    }
}