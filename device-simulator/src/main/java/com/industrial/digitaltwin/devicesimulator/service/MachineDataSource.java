package com.industrial.digitaltwin.devicesimulator.service;


public interface MachineDataSource {
    void startDataGeneration(Runnable dataCallback);
    void initialize();
    void shutdown();
    String getSourceType();
}