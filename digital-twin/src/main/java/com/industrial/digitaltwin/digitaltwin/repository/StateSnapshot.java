package com.industrial.digitaltwin.digitaltwin.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.digitaltwin.digitaltwin.model.DigitalTwinState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "state_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "machine_id", nullable = false)
    private String machineId;
    
    @Column(name = "snapshot_time", nullable = false)
    private Instant snapshotTime;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> stateData;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    // Method to convert DigitalTwinState to StateSnapshot
    public static StateSnapshot fromDigitalTwinState(DigitalTwinState state, ObjectMapper objectMapper) {
        try {
            // Convert DigitalTwinState to a map that can be stored as JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> stateMap = objectMapper.convertValue(state, Map.class);
            
            return StateSnapshot.builder()
                .machineId(state.getMachineId())
                .snapshotTime(state.getLastUpdated())
                .stateData(stateMap)
                .createdAt(Instant.now())
                .build();
        } catch (Exception e) {
            // Fallback to basic information if conversion fails
            Map<String, Object> basicData = new HashMap<>();
            basicData.put("machineId", state.getMachineId());
            basicData.put("lastUpdated", state.getLastUpdated());
            basicData.put("status", state.getStatus());
            
            return StateSnapshot.builder()
                .machineId(state.getMachineId())
                .snapshotTime(state.getLastUpdated())
                .stateData(basicData)
                .createdAt(Instant.now())
                .build();
        }
    }
    
    // Convenience method that doesn't require ObjectMapper
    public static StateSnapshot fromDigitalTwinState(DigitalTwinState state) {
        // For now, just store basic information
        Map<String, Object> basicData = new HashMap<>();
        basicData.put("machineId", state.getMachineId());
        basicData.put("lastUpdated", state.getLastUpdated());
        basicData.put("status", state.getStatus());
        basicData.put("operationalHours", state.getOperationalHours());
        basicData.put("efficiencyRating", state.getEfficiencyRating());
        
        return StateSnapshot.builder()
            .machineId(state.getMachineId())
            .snapshotTime(state.getLastUpdated())
            .stateData(basicData)
            .createdAt(Instant.now())
            .build();
    }
}