package com.industrial.digitaltwin.alertanalytics.repository;

import com.industrial.digitaltwin.alertanalytics.entity.HistoricalDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface HistoricalDataRepository extends JpaRepository<HistoricalDataEntity, Long> {
    
    List<HistoricalDataEntity> findByMachineId(String machineId);
    
    List<HistoricalDataEntity> findByMachineIdAndTimestampBetween(
        String machineId, 
        Instant startTime, 
        Instant endTime
    );
    
    @Query("SELECT h FROM HistoricalDataEntity h WHERE h.machineId = :machineId AND h.timestamp > :since ORDER BY h.timestamp ASC")
    List<HistoricalDataEntity> findByMachineIdAndTimestampAfterOrderByTimestampAsc(
        String machineId, 
        Instant since
    );
    
    List<HistoricalDataEntity> findByMachineIdOrderByTimestampDesc(String machineId);
    
    @Query("SELECT MIN(h.timestamp) FROM HistoricalDataEntity h WHERE h.machineId = :machineId")
    java.util.Optional<java.time.Instant> findMinTimestampByMachineId(String machineId);
    
    @Query("SELECT MAX(h.timestamp) FROM HistoricalDataEntity h WHERE h.machineId = :machineId")
    java.util.Optional<java.time.Instant> findMaxTimestampByMachineId(String machineId);
}