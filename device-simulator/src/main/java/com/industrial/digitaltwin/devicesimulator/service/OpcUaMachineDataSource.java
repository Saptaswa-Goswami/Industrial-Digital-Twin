package com.industrial.digitaltwin.devicesimulator.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;


@Component("opc-ua")
@ConditionalOnProperty(name = "app.data-source.mode", havingValue = "opc-ua")
@Slf4j
public class OpcUaMachineDataSource implements MachineDataSource {

    @Override
    public void startDataGeneration(Runnable dataCallback) {
        log.warn("OPC-UA data source is not yet implemented. Using simulator instead.");
        // Future implementation for real OPC-UA machines
    }

    @Override
    public void initialize() {
        log.info("Initializing OPC-UA Machine Data Source");
        // Initialize OPC-UA client connections
    }

    @Override
    public void shutdown() {
        log.info("Shutting down OPC-UA Machine Data Source");
        // Cleanup OPC-UA connections
    }

    @Override
    public String getSourceType() {
        return "opc-ua";
    }
}