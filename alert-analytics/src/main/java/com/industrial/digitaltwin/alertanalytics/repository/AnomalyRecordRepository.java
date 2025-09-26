package com.industrial.digitaltwin.alertanalytics.repository;

import com.industrial.digitaltwin.alertanalytics.entity.AnomalyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnomalyRecordRepository extends JpaRepository<AnomalyRecordEntity, Long> {
    
    List<AnomalyRecordEntity> findByMachineId(String machineId);
    
    List<AnomalyRecordEntity> findByAnomalyType(String anomalyType);
    
    List<AnomalyRecordEntity> findBySeverity(String severity);
    
    @Query("SELECT a FROM AnomalyRecordEntity a WHERE a.machineId = :machineId AND a.timestamp > :since ORDER BY a.timestamp DESC")
    List<AnomalyRecordEntity> findByMachineIdAndTimestampAfter(String machineId, Instant since);
    
    @Query("SELECT a FROM AnomalyRecordEntity a WHERE a.timestamp > :since ORDER BY a.timestamp DESC")
    List<AnomalyRecordEntity> findByTimestampAfter(Instant since);
}