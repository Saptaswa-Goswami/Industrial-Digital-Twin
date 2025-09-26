package com.industrial.digitaltwin.digitaltwin.controller;

import com.industrial.digitaltwin.digitaltwin.model.DigitalTwinState;
import com.industrial.digitaltwin.digitaltwin.service.StateManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/digital-twin")
@Slf4j
public class DigitalTwinController {

    @Autowired
    private StateManagementService stateManagementService;

    @GetMapping("/state/{machineId}")
    public ResponseEntity<DigitalTwinState> getMachineState(@PathVariable String machineId) {
        DigitalTwinState state = stateManagementService.getState(machineId);
        if (state != null) {
            return ResponseEntity.ok(state);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/states")
    public ResponseEntity<Map<String, DigitalTwinState>> getAllMachineStates() {
        return ResponseEntity.ok(stateManagementService.getAllStates());
    }

    @DeleteMapping("/state/{machineId}")
    public ResponseEntity<Map<String, Object>> clearMachineState(@PathVariable String machineId) {
        DigitalTwinState existingState = stateManagementService.getState(machineId);
        if (existingState != null) {
            stateManagementService.clearState(machineId);
            Map<String, Object> response = Map.of(
                "message", "Machine state cleared successfully",
                "machineId", machineId,
                "previousStateStatus", existingState.getStatus(),
                "previousLastUpdated", existingState.getLastUpdated(),
                "timestamp", Instant.now()
            );
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = Map.of(
                "message", "Machine not found, nothing to clear",
                "machineId", machineId,
                "timestamp", Instant.now()
            );
            return ResponseEntity.status(404).body(response);
        }
    }
}