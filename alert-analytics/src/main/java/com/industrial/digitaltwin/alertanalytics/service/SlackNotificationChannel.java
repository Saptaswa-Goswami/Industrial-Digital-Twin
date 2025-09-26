package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.model.AlertEvent;
import com.industrial.digitaltwin.alertanalytics.model.NotificationChannel;
import com.industrial.digitaltwin.alertanalytics.model.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("slack")
@ConditionalOnProperty(name = "app.notification.channels", havingValue = "slack")
@Slf4j
public class SlackNotificationChannel implements NotificationChannel {

    @Override
    public void sendNotification(AlertEvent alert) {
        // Future implementation with Slack notifications
        log.info("Slack notification would be sent for alert: {}", alert.getAlertId());
    }

    @Override
    public boolean isAvailable() {
        return true; // Would check actual Slack integration status
    }

    @Override
    public NotificationType getType() {
        return NotificationType.SLACK;
    }
}