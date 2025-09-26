package com.industrial.digitaltwin.alertanalytics.entity;

import com.industrial.digitaltwin.alertanalytics.model.NotificationStatus;
import com.industrial.digitaltwin.alertanalytics.model.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "notification_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id")
    private String alertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType; // EMAIL, SMS, SLACK, etc.

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject", length = 25)
    private String subject;

    @Column(name = "content", length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status; // SENT, FAILED, DELIVERED

    @Column(name = "sent_time", nullable = false)
    private Instant sentTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_details", columnDefinition = "jsonb")
    private String responseDetails; // Provider-specific response

    @Column(name = "created_at")
    private Instant createdAt;
}