package com.industrial.digitaltwin.alertanalytics.repository;

import com.industrial.digitaltwin.alertanalytics.entity.PerformanceMetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PerformanceMetricsRepository extends JpaRepository<PerformanceMetricsEntity, Long> {
    
    List<PerformanceMetricsEntity> findByMachineId(String machineId);
    
    @Query("SELECT p FROM PerformanceMetricsEntity p WHERE p.machineId = :machineId AND p.calculationTime > :since ORDER BY p.calculationTime DESC")
    List<PerformanceMetricsEntity> findByMachineIdAndCalculationTimeAfter(String machineId, Instant since);
    
    @Query("SELECT p FROM PerformanceMetricsEntity p WHERE p.calculationTime > :since ORDER BY p.calculationTime DESC")
    List<PerformanceMetricsEntity> findByCalculationTimeAfter(Instant since);
}