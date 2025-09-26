package com.industrial.digitaltwin.devicesimulator.controller;

import com.industrial.digitaltwin.devicesimulator.config.SimulationConfig;
import com.industrial.digitaltwin.devicesimulator.service.MachineDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final MachineDataService machineDataService;
    private final SimulationConfig simulationConfig;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSimulationStatus() {
        Map<String, Object> status = Map.of(
            "activeDataSource", machineDataService.getActiveDataSourceType(),
            "isRunning", true, // In a real implementation, we'd track this state
            "machineCount", simulationConfig.getMachines().size(),
            "samplingInterval", simulationConfig.getSamplingInterval(),
            "anomalyProbability", simulationConfig.getAnomalyProbability()
        );
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/machines")
    public ResponseEntity<?> getMachines() {
        return ResponseEntity.ok(simulationConfig.getMachines());
    }
}