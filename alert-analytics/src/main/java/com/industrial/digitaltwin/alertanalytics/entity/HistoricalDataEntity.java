package com.industrial.digitaltwin.alertanalytics.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "historical_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalDataEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "machine_id", nullable = false)
    private String machineId;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "temperature")
    private Double temperature;
    
    @Column(name = "vibration")
    private Double vibration;
    
    @Column(name = "load")
    private Double load;
    
    @Column(name = "pressure")
    private Double pressure;
    
    @Column(name = "rpm")
    private Double rpm;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "anomaly_score")
    private Double anomalyScore;
    
    @Column(name = "alert_id")
    private String alertId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}