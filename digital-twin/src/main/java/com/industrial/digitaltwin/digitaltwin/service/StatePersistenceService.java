package com.industrial.digitaltwin.digitaltwin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.digitaltwin.digitaltwin.config.StateStoreConfig;
import com.industrial.digitaltwin.digitaltwin.model.DigitalTwinState;
import com.industrial.digitaltwin.digitaltwin.repository.StateSnapshot;
import com.industrial.digitaltwin.digitaltwin.repository.StateSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
public class StatePersistenceService {

    @Autowired
    private StateManagementService stateManagementService;
    
    @Autowired
    private StateStoreConfig config;
    
    @Autowired
    private StateSnapshotRepository stateSnapshotRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Scheduled method to persist state snapshots to database at configured intervals
     */
    @Scheduled(fixedRateString = "#{@stateStoreConfig.snapshotInterval.toMillis()}")
    public void persistStateSnapshots() {
        log.info("Starting state snapshot persistence");
        
        Map<String, DigitalTwinState> allStates = stateManagementService.getAllStates();
        
        for (Map.Entry<String, DigitalTwinState> entry : allStates.entrySet()) {
            String machineId = entry.getKey();
            DigitalTwinState state = entry.getValue();
            
            try {
                // Save state snapshot to database
                StateSnapshot snapshot = StateSnapshot.fromDigitalTwinState(state, objectMapper);
                
                stateSnapshotRepository.save(snapshot);
                log.debug("Persisted state snapshot for machine: {} at {}",
                        machineId, Instant.now());
                
            } catch (Exception e) {
                log.error("Error persisting state for machine: {}", machineId, e);
            }
        }
        
        log.info("Completed state snapshot persistence for {} machines", allStates.size());
    }

    /**
     * Method to recover state from database on startup
     */
    public void recoverStateFromDatabase() {
        log.info("Recovering state from database");
        
        // In a real implementation, this would load state snapshots from the database
        // For now, we'll just log the action
        log.info("State recovery completed");
    }
}