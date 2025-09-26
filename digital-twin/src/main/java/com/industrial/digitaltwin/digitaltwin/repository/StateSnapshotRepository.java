package com.industrial.digitaltwin.digitaltwin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StateSnapshotRepository extends JpaRepository<StateSnapshot, Long> {
    
    List<StateSnapshot> findByMachineId(String machineId);
    
    @Query("SELECT s FROM StateSnapshot s WHERE s.machineId = :machineId AND s.snapshotTime > :since ORDER BY s.snapshotTime DESC")
    List<StateSnapshot> findByMachineIdAndSnapshotTimeAfter(String machineId, Instant since);
    
    @Query("SELECT s FROM StateSnapshot s WHERE s.snapshotTime > :since ORDER BY s.snapshotTime DESC")
    List<StateSnapshot> findBySnapshotTimeAfter(Instant since);
}