package com.industrial.digitaltwin.digitaltwin.service;

import com.industrial.digitaltwin.digitaltwin.model.DigitalTwinState;
import com.industrial.digitaltwin.digitaltwin.model.MachineTelemetry;
import com.industrial.digitaltwin.digitaltwin.model.AnomalyEvent;

import java.util.List;

public interface AnomalyDetectionStrategy {
    List<com.industrial.digitaltwin.digitaltwin.model.AnomalyRecord> detect(DigitalTwinState currentState, MachineTelemetry newTelemetry);
    String getStrategyName();
}