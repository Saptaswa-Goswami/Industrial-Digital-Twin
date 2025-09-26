package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.model.AlertEvent;
import com.industrial.digitaltwin.alertanalytics.model.NotificationChannel;
import com.industrial.digitaltwin.alertanalytics.model.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("microsoft-teams")
@ConditionalOnProperty(name = "app.notification.channels", havingValue = "msteams")
@Slf4j
public class MicrosoftTeamsNotificationChannel implements NotificationChannel {

    @Override
    public void sendNotification(AlertEvent alert) {
        // Future implementation with Microsoft Teams notifications
        log.info("Microsoft Teams notification would be sent for alert: {}", alert.getAlertId());
    }

    @Override
    public boolean isAvailable() {
        return true; // Would check actual Teams integration status
    }

    @Override
    public NotificationType getType() {
        return NotificationType.MICROSOFT_TEAMS;
    }
}