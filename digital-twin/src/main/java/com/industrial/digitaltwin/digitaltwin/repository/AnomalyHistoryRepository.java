package com.industrial.digitaltwin.digitaltwin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnomalyHistoryRepository extends JpaRepository<AnomalyHistory, Long> {
    
    List<AnomalyHistory> findByMachineId(String machineId);
    
    List<AnomalyHistory> findByMachineIdAndTimestampAfter(String machineId, Instant timestamp);
    
    @Query("SELECT a FROM AnomalyHistory a WHERE a.machineId = :machineId AND a.timestamp > :since AND a.severity = 'CRITICAL' ORDER BY a.timestamp DESC")
    List<AnomalyHistory> findCriticalAnomaliesSince(String machineId, Instant since);
    
    List<AnomalyHistory> findBySeverity(String severity);
}