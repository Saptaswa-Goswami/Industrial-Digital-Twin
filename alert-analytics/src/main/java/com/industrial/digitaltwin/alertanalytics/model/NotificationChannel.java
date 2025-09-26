package com.industrial.digitaltwin.alertanalytics.model;

public interface NotificationChannel {
    void sendNotification(AlertEvent alert);
    boolean isAvailable();
    NotificationType getType();
}