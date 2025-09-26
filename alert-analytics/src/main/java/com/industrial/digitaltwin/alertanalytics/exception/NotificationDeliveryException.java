package com.industrial.digitaltwin.alertanalytics.exception;

/**
 * Exception for notification delivery errors
 */
public class NotificationDeliveryException extends AlertAnalyticsException {
    
    public NotificationDeliveryException(String message) {
        super(message);
    }
    
    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}