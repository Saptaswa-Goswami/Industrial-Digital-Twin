package com.industrial.digitaltwin.alertanalytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "performance_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false)
    private String machineId;

    @Column(name = "calculation_time", nullable = false)
    private Instant calculationTime;

    @Column(name = "average_load")
    private Double averageLoad;

    @Column(name = "average_temperature")
    private Double averageTemperature;

    @Column(name = "average_vibration")
    private Double averageVibration;

    @Column(name = "uptime_percentage")
    private Double uptimePercentage;

    @Column(name = "anomaly_count")
    private Integer anomalyCount;

    @Column(name = "efficiency_rating")
    private Double efficiencyRating;

    @Column(name = "created_at")
    private Instant createdAt;
}