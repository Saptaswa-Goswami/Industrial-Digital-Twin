package com.industrial.digitaltwin.alertanalytics.model;

public enum AlertSeverity {
    CRITICAL(1), WARNING(2), INFO(3);
    
    private final int priority;
    
    AlertSeverity(int priority) { 
        this.priority = priority; 
    }
    
    public int getPriority() {
        return priority;
    }
}