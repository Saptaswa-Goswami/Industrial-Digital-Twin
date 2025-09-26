package com.industrial.digitaltwin.digitaltwin.service;

import com.industrial.digitaltwin.digitaltwin.config.StateStoreConfig;
import com.industrial.digitaltwin.digitaltwin.model.DigitalTwinState;
import com.industrial.digitaltwin.digitaltwin.model.MachineStatus;
import com.industrial.digitaltwin.digitaltwin.model.MachineTelemetry;
import com.industrial.digitaltwin.digitaltwin.model.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
public class StateManagementService {

    @Autowired
    private StateStoreConfig stateStoreConfig;

    private final Map<String, DigitalTwinState> stateStore = new ConcurrentHashMap<>();
    
    // Using a circular buffer implementation for historical data to optimize memory usage
    private final Map<String, Queue<SensorData>> historicalDataBuffers = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        log.info("Initializing State Management Service with max entries: {}",
                stateStoreConfig.getStateStore().getMaxEntries());
    }

    public DigitalTwinState getState(String machineId) {
        return stateStore.get(machineId);
    }

    public DigitalTwinState updateState(String machineId, MachineTelemetry telemetry) {
        DigitalTwinState currentState = stateStore.computeIfAbsent(machineId, id ->
            createInitialState(id));
        
        // Update state with new telemetry
        currentState.setLastUpdated(Instant.now());
        currentState.setCurrentSensorData(telemetry.getSensorData());
        currentState.setMachineId(machineId);
        // Update status if present in telemetry
        if (telemetry.getStatus() != null) {
            currentState.setStatus(telemetry.getStatus());
        }
        
        // Apply status-based business logic
        applyStatusBasedLogic(currentState, telemetry);
        
        // Update circular buffer for historical data
        Queue<SensorData> buffer = historicalDataBuffers.computeIfAbsent(machineId, k -> new ConcurrentLinkedQueue<>());
        
        // Add new data point
        buffer.offer(telemetry.getSensorData());
        
        // Maintain buffer size (circular buffer)
        int maxHistorySize = 100; // Configurable
        while (buffer.size() > maxHistorySize) {
            buffer.poll(); // Remove oldest element
        }
        
        // Update the historical data in the state object
        currentState.setHistoricalData(new ArrayList<>(buffer));
        
        // Update computed metrics
        updateComputedMetrics(currentState);
        
        // Apply eviction policy
        applyEvictionPolicy();
        
        return currentState;
    }

    private DigitalTwinState createInitialState(String machineId) {
        return DigitalTwinState.builder()
            .machineId(machineId)
            .lastUpdated(Instant.now())
            .status(MachineStatus.NORMAL) // Default to normal status
            .operationalHours(0)
            .efficiencyRating(100.0)
            .build();
    }

    private void updateComputedMetrics(DigitalTwinState state) {
        // Placeholder for computed metrics calculation
        // This will be enhanced in future phases
    }
    
    private void applyStatusBasedLogic(DigitalTwinState state, MachineTelemetry telemetry) {
        // Apply different business logic based on the machine status
        switch (state.getStatus()) {
            case ERROR:
                // In ERROR state, mark machine as non-operational
                state.setOperationalHours(state.getOperationalHours() - 1); // Don't accumulate operational hours during error
                break;
            case CRITICAL:
                // In CRITICAL state, reduce efficiency rating
                state.setEfficiencyRating(Math.max(10.0, state.getEfficiencyRating() - 5.0));
                break;
            case WARNING:
                // In WARNING state, slightly reduce efficiency rating
                state.setEfficiencyRating(Math.max(10.0, state.getEfficiencyRating() - 1.0));
                break;
            case MAINTENANCE:
                // In MAINTENANCE state, mark as non-operational
                state.setOperationalHours(state.getOperationalHours() - 1); // Don't accumulate operational hours during maintenance
                break;
            case OFFLINE:
                // In OFFLINE state, mark as non-operational
                state.setOperationalHours(state.getOperationalHours() - 1); // Don't accumulate operational hours when offline
                break;
            case IDLE:
                // In IDLE state, reduce operational hours accumulation rate
                // This would be handled by other logic that tracks time, but we can set a flag
                break;
            case PEAK_LOAD:
                // In PEAK_LOAD state, adjust efficiency to account for higher stress
                state.setEfficiencyRating(Math.max(10.0, state.getEfficiencyRating() - 0.5));
                break;
            case NORMAL:
            default:
                // In NORMAL state, increment operational hours and maintain efficiency
                state.setOperationalHours(state.getOperationalHours() + 1);
                break;
        }
    }

    private void applyEvictionPolicy() {
        // Remove entries that exceed the maximum count
        if (stateStore.size() > stateStoreConfig.getStateStore().getMaxEntries()) {
            // This is a simple approach - in production, we might want to evict based on last access time
            int excessCount = stateStore.size() - stateStoreConfig.getStateStore().getMaxEntries();
            Iterator<Map.Entry<String, DigitalTwinState>> iterator = stateStore.entrySet().iterator();
            for (int i = 0; i < excessCount && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    public Map<String, DigitalTwinState> getAllStates() {
        return new ConcurrentHashMap<>(stateStore);
    }

    public void clearState(String machineId) {
        stateStore.remove(machineId);
        historicalDataBuffers.remove(machineId);
    }
    
    public int getCurrentStateCount() {
        return stateStore.size();
    }
    
    public long getMemoryUsage() {
        // This is a simplified memory usage estimation
        // In a real system, we'd use more sophisticated memory monitoring
        return stateStore.size() * 1024L; // Estimate ~1KB per state
    }
}