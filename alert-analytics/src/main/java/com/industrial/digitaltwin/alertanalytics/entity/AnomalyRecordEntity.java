package com.industrial.digitaltwin.alertanalytics.entity;

import com.industrial.digitaltwin.alertanalytics.model.AlertSeverity;
import com.industrial.digitaltwin.alertanalytics.model.AlertType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "anomaly_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false)
    private String machineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", nullable = false)
    private AlertType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sensor_data", columnDefinition = "jsonb")
    private Map<String, Object> sensorData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "calculated_metrics", columnDefinition = "jsonb")
    private Map<String, Object> calculatedMetrics;

    @Column(name = "created_at")
    private Instant createdAt;
}