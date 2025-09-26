package com.industrial.digitaltwin.alertanalytics.exception;

/**
 * Custom exception class for Alert/Analytics Service
 */
public class AlertAnalyticsException extends RuntimeException {
    
    public AlertAnalyticsException(String message) {
        super(message);
    }
    
    public AlertAnalyticsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public AlertAnalyticsException(Throwable cause) {
        super(cause);
    }
}