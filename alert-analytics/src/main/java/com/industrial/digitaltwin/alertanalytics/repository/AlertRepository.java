package com.industrial.digitaltwin.alertanalytics.repository;

import com.industrial.digitaltwin.alertanalytics.entity.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {
    
    List<AlertEntity> findByMachineId(String machineId);
    
    List<AlertEntity> findBySeverity(com.industrial.digitaltwin.alertanalytics.model.AlertSeverity severity);
    
    List<AlertEntity> findByStatus(com.industrial.digitaltwin.alertanalytics.model.AlertStatus status);
    
    List<AlertEntity> findByMachineIdAndSeverity(String machineId, com.industrial.digitaltwin.alertanalytics.model.AlertSeverity severity);
    
    List<AlertEntity> findByMachineIdAndStatus(String machineId, com.industrial.digitaltwin.alertanalytics.model.AlertStatus status);
    
    List<AlertEntity> findBySeverityAndStatus(com.industrial.digitaltwin.alertanalytics.model.AlertSeverity severity, com.industrial.digitaltwin.alertanalytics.model.AlertStatus status);
    
    List<AlertEntity> findByMachineIdAndSeverityAndStatus(String machineId, com.industrial.digitaltwin.alertanalytics.model.AlertSeverity severity, com.industrial.digitaltwin.alertanalytics.model.AlertStatus status);
    
    @Query("SELECT a FROM AlertEntity a WHERE a.alertId = :alertId")
    java.util.Optional<AlertEntity> findByAlertId(String alertId);
    
    @Query("SELECT a FROM AlertEntity a WHERE a.machineId = :machineId AND a.timestamp > :since ORDER BY a.timestamp DESC")
    List<AlertEntity> findByMachineIdAndTimestampAfter(String machineId, Instant since);
    
    @Query("SELECT a FROM AlertEntity a WHERE a.timestamp > :since ORDER BY a.timestamp DESC")
    List<AlertEntity> findByTimestampAfter(Instant since);
}