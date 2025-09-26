package com.industrial.digitaltwin.alertanalytics.exception;

/**
 * Exception for analytics calculation errors
 */
public class AnalyticsCalculationException extends AlertAnalyticsException {
    
    public AnalyticsCalculationException(String message) {
        super(message);
    }
    
    public AnalyticsCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}