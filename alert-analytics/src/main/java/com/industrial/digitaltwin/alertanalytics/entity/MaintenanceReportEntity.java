package com.industrial.digitaltwin.alertanalytics.entity;

import com.industrial.digitaltwin.alertanalytics.model.ReportType;
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
@Table(name = "maintenance_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", unique = true, nullable = false)
    private String reportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType; // DAILY, WEEKLY, MONTHLY

    @Column(name = "generated_time", nullable = false)
    private Instant generatedTime;

    @Column(name = "machine_id")
    private String machineId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> content; // Report content in JSON format

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics_summary", columnDefinition = "jsonb")
    private Map<String, Object> metricsSummary; // Summary of key metrics

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendations", columnDefinition = "jsonb")
    private Map<String, Object> recommendations; // Maintenance recommendations

    @Column(name = "created_at")
    private Instant createdAt;
}