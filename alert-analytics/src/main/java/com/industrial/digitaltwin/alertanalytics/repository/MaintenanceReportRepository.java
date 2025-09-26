package com.industrial.digitaltwin.alertanalytics.repository;

import com.industrial.digitaltwin.alertanalytics.entity.MaintenanceReportEntity;
import com.industrial.digitaltwin.alertanalytics.model.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MaintenanceReportRepository extends JpaRepository<MaintenanceReportEntity, Long> {
    
    List<MaintenanceReportEntity> findByReportType(ReportType reportType);
    
    List<MaintenanceReportEntity> findByMachineId(String machineId);
    
    @Query("SELECT m FROM MaintenanceReportEntity m WHERE m.machineId = :machineId AND m.generatedTime > :since ORDER BY m.generatedTime DESC")
    List<MaintenanceReportEntity> findByMachineIdAndGeneratedTimeAfter(String machineId, Instant since);
    
    @Query("SELECT m FROM MaintenanceReportEntity m WHERE m.generatedTime > :since ORDER BY m.generatedTime DESC")
    List<MaintenanceReportEntity> findByGeneratedTimeAfter(Instant since);
}