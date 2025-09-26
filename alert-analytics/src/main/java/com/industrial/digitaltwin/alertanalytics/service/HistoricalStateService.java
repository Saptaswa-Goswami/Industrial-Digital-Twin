package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.entity.HistoricalDataEntity;
import com.industrial.digitaltwin.alertanalytics.model.MachineTelemetry;
import com.industrial.digitaltwin.alertanalytics.model.SensorData;
import com.industrial.digitaltwin.alertanalytics.repository.HistoricalDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricalStateService {

    private final HistoricalDataRepository historicalDataRepository;

    /**
     * Reconstructs the machine state at a specific point in time
     */
    public MachineTelemetry getHistoricalStateAtTime(String machineId, Instant timestamp) {
        log.debug("Getting historical state for machine: {} at time: {}", machineId, timestamp);
        
        // Find the closest historical data point before or at the specified time
        List<HistoricalDataEntity> dataPoints = historicalDataRepository
            .findByMachineIdAndTimestampBetween(
                machineId,
                timestamp.minusSeconds(1), // Look for data within 1 second before
                timestamp.plusSeconds(1)   // and 1 second after the target time
            );

        if (dataPoints.isEmpty()) {
            log.warn("No historical data found for machine: {} at time: {}", machineId, timestamp);
            return createDefaultTelemetry(machineId, timestamp);
        }

        // Find the closest data point to the target time
        HistoricalDataEntity closestData = dataPoints.stream()
            .min((d1, d2) -> Math.abs(d1.getTimestamp().compareTo(timestamp)) 
                - Math.abs(d2.getTimestamp().compareTo(timestamp)))
            .orElse(dataPoints.get(0));

        return convertToMachineTelemetry(closestData);
    }

    /**
     * Reconstructs the machine state timeline between two timestamps
     */
    public List<MachineTelemetry> getHistoricalStateTimeline(String machineId, Instant startTime, Instant endTime) {
        log.debug("Getting historical timeline for machine: {} from: {} to: {}", machineId, startTime, endTime);

        List<HistoricalDataEntity> dataPoints = historicalDataRepository
            .findByMachineIdAndTimestampBetween(machineId, startTime, endTime);

        return dataPoints.stream()
            .map(this::convertToMachineTelemetry)
            .sorted((t1, t2) -> t1.getTimestamp().compareTo(t2.getTimestamp()))
            .collect(Collectors.toList());
    }

    /**
     * Gets the most recent state before a specific time
     */
    public MachineTelemetry getMostRecentStateBefore(String machineId, Instant timestamp) {
        log.debug("Getting most recent state before time: {} for machine: {}", timestamp, machineId);

        List<HistoricalDataEntity> dataPoints = historicalDataRepository
            .findByMachineIdAndTimestampAfterOrderByTimestampAsc(machineId, timestamp.minusSeconds(3600)); // Look back 1 hour max

        if (dataPoints.isEmpty()) {
            log.warn("No historical data found before time: {} for machine: {}", timestamp, machineId);
            return createDefaultTelemetry(machineId, timestamp);
        }

        // Find the last data point before the target time
        return dataPoints.stream()
            .filter(data -> data.getTimestamp().isBefore(timestamp))
            .max((d1, d2) -> d1.getTimestamp().compareTo(d2.getTimestamp()))
            .map(this::convertToMachineTelemetry)
            .orElse(createDefaultTelemetry(machineId, timestamp));
    }

    private MachineTelemetry convertToMachineTelemetry(HistoricalDataEntity entity) {
        SensorData sensorData = SensorData.builder()
            .temperature(entity.getTemperature() != null ? entity.getTemperature() : 0.0)
            .vibration(entity.getVibration() != null ? entity.getVibration() : 0.0)
            .load(entity.getLoad() != null ? entity.getLoad() : 0.0)
            .pressure(entity.getPressure() != null ? entity.getPressure() : 0.0)
            .rpm(entity.getRpm() != null ? entity.getRpm() : 0.0)
            .timestamp(entity.getTimestamp())
            .build();

        return MachineTelemetry.builder()
            .machineId(entity.getMachineId())
            .timestamp(entity.getTimestamp())
            .sensorData(sensorData)
            .status(entity.getStatus() != null ?
                com.industrial.digitaltwin.alertanalytics.model.MachineStatus.valueOf(entity.getStatus()) :
                com.industrial.digitaltwin.alertanalytics.model.MachineStatus.NORMAL)
            .schemaVersion("1.0")
            .additionalMetrics(new java.util.HashMap<>())
            .dataSourceType("historical")
            .dataQualityScore(1.0)
            .build();
    }

    private MachineTelemetry createDefaultTelemetry(String machineId, Instant timestamp) {
        return MachineTelemetry.builder()
            .machineId(machineId)
            .timestamp(timestamp)
            .sensorData(SensorData.builder()
                .temperature(0.0)
                .vibration(0.0)
                .load(0.0)
                .pressure(0.0)
                .rpm(0.0)
                .build())
            .status(com.industrial.digitaltwin.alertanalytics.model.MachineStatus.NORMAL)
            .schemaVersion("1.0")
            .additionalMetrics(new java.util.HashMap<>())
            .dataSourceType("historical")
            .dataQualityScore(1.0)
            .build();
    }
    
    /**
     * Gets the minimum timestamp for a specific machine
     */
    public java.util.Optional<java.time.Instant> getMinTimestampForMachine(String machineId) {
        java.util.Optional<java.time.Instant> minTimestamp = historicalDataRepository.findMinTimestampByMachineId(machineId);
        return minTimestamp;
    }
    
    /**
     * Gets the maximum timestamp for a specific machine
     */
    public java.util.Optional<java.time.Instant> getMaxTimestampForMachine(String machineId) {
        java.util.Optional<java.time.Instant> maxTimestamp = historicalDataRepository.findMaxTimestampByMachineId(machineId);
        return maxTimestamp;
    }
}