package com.industrial.digitaltwin.alertanalytics.exception;

/**
 * Exception for data persistence errors
 */
public class DataPersistenceException extends AlertAnalyticsException {
    
    public DataPersistenceException(String message) {
        super(message);
    }
    
    public DataPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}