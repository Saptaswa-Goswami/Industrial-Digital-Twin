package com.industrial.digitaltwin.alertanalytics.exception;

/**
 * Exception for alert processing errors
 */
public class AlertProcessingException extends AlertAnalyticsException {
    
    public AlertProcessingException(String message) {
        super(message);
    }
    
    public AlertProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}