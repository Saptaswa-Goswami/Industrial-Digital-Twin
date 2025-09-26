package com.industrial.digitaltwin.digitaltwin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyEvent {
    private String machineId;
    private List<AnomalyRecord> anomalies;
    private Instant timestamp;
    private String algorithmUsed;
}