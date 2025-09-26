package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.model.AlertEvent;
import com.industrial.digitaltwin.alertanalytics.model.NotificationChannel;
import com.industrial.digitaltwin.alertanalytics.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("email")
    private NotificationChannel emailChannel;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("sms")
    private NotificationChannel smsChannel;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("slack")
    private NotificationChannel slackChannel;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("microsoft-teams")
    private NotificationChannel microsoftTeamsChannel;

    private final Map<String, NotificationChannel> channelMap = new ConcurrentHashMap<>();

    // Initialize the channel map after all beans are created
    @PostConstruct
    private void init() {
        if (emailChannel != null) channelMap.put("email", emailChannel);
        if (smsChannel != null) channelMap.put("sms", smsChannel);
        if (slackChannel != null) channelMap.put("slack", slackChannel);
        if (microsoftTeamsChannel != null) channelMap.put("microsoft-teams", microsoftTeamsChannel);
    }

    public void sendNotification(AlertEvent alert, List<NotificationType> channels) {
        for (NotificationType channelType : channels) {
            NotificationChannel channel = getChannelByType(channelType);
            if (channel != null && channel.isAvailable()) {
                try {
                    channel.sendNotification(alert);
                    log.debug("Notification sent via {} for alert: {}", channelType, alert.getAlertId());
                } catch (Exception e) {
                    log.error("Failed to send notification via {} for alert: {}", channelType, alert.getAlertId(), e);
                }
            } else {
                log.warn("Notification channel {} is not available for alert: {}", channelType, alert.getAlertId());
            }
        }
    }

    public void sendNotification(AlertEvent alert) {
        // By default, send notifications via all available channels
        List<NotificationType> allChannels = List.of(
            NotificationType.EMAIL,
            NotificationType.SMS
            // Add other channels as they become available
        );
        sendNotification(alert, allChannels);
    }

    public void sendCriticalNotification(AlertEvent alert) {
        // For critical alerts, send notifications via all available channels with priority
        List<NotificationType> criticalChannels = List.of(
            NotificationType.EMAIL,
            NotificationType.SMS,
            NotificationType.SLACK,
            NotificationType.MICROSOFT_TEAMS
        );
        sendNotification(alert, criticalChannels);
    }

    private NotificationChannel getChannelByType(NotificationType type) {
        switch (type) {
            case EMAIL:
                return channelMap.get("email");
            case SMS:
                return channelMap.get("sms");
            case SLACK:
                return channelMap.get("slack");
            case MICROSOFT_TEAMS:
                return channelMap.get("microsoft-teams");
            default:
                log.warn("Unknown notification type: {}", type);
                return null;
        }
    }

    public boolean isChannelAvailable(NotificationType type) {
        NotificationChannel channel = getChannelByType(type);
        return channel != null && channel.isAvailable();
    }
}