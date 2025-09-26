package com.industrial.digitaltwin.alertanalytics.repository;

import com.industrial.digitaltwin.alertanalytics.entity.NotificationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, Long> {
    
    List<NotificationLogEntity> findByAlertId(String alertId);
    
    List<NotificationLogEntity> findByNotificationType(String notificationType);
    
    List<NotificationLogEntity> findByStatus(String status);
    
    @Query("SELECT n FROM NotificationLogEntity n WHERE n.alertId = :alertId AND n.sentTime > :since ORDER BY n.sentTime DESC")
    List<NotificationLogEntity> findByAlertIdAndSentTimeAfter(String alertId, Instant since);
    
    @Query("SELECT n FROM NotificationLogEntity n WHERE n.sentTime > :since ORDER BY n.sentTime DESC")
    List<NotificationLogEntity> findBySentTimeAfter(Instant since);
}