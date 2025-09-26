package com.industrial.digitaltwin.alertanalytics.service;

import com.industrial.digitaltwin.alertanalytics.config.AlertAnalyticsProperties;
import com.industrial.digitaltwin.alertanalytics.entity.AlertEntity;
import com.industrial.digitaltwin.alertanalytics.model.AlertEvent;
import com.industrial.digitaltwin.alertanalytics.model.AlertSeverity;
import com.industrial.digitaltwin.alertanalytics.model.AlertStatus;
import com.industrial.digitaltwin.alertanalytics.model.AlertType;
import com.industrial.digitaltwin.alertanalytics.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertProcessingService {

    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final AlertAnalyticsProperties properties;
    private final MetricsService metricsService;

    // Cache for deduplication - maps machineId + alertType to timestamp
    private final Map<String, Instant> alertDeduplicationCache = new ConcurrentHashMap<>();
    
    // Cache for correlation - groups related alerts by machine
    private final Map<String, List<String>> correlationCache = new ConcurrentHashMap<>();

    public AlertEvent processAlert(AlertEvent alertEvent) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Check for duplicates
            if (isDuplicateAlert(alertEvent)) {
                log.debug("Duplicate alert detected and filtered: {}", alertEvent.getAlertId());
                return null; // Don't process duplicate alerts
            }

            // Step 2: Correlate with existing alerts
            correlateAlert(alertEvent);

            // Step 3: Create alert entity
            AlertEntity alertEntity = convertToAlertEntity(alertEvent);
            
            // Step 4: Save to database
            AlertEntity savedAlert = alertRepository.save(alertEntity);
            
            // Step 5: Update deduplication cache
            updateDeduplicationCache(alertEvent);
            
            // Step 6: Send notifications
            notificationService.sendNotification(alertEvent);
            
            // Record metrics
            long processingTime = System.currentTimeMillis() - startTime;
            metricsService.incrementAlertsProcessed();
            metricsService.recordAlertProcessingTime(processingTime);
            
            log.info("Alert processed and saved: {} in {} ms", savedAlert.getAlertId(), processingTime);
            
            return convertToAlertEvent(savedAlert);
        } catch (Exception e) {
            log.error("Error processing alert: {}", alertEvent.getAlertId(), e);
            throw e;
        }
    }

    private boolean isDuplicateAlert(AlertEvent alertEvent) {
        String cacheKey = generateCacheKey(alertEvent);
        Instant lastAlertTime = alertDeduplicationCache.get(cacheKey);
        
        if (lastAlertTime != null) {
            // Check if the time since last alert is within the deduplication window
            Instant now = Instant.now();
            long timeDiffMillis = java.time.Duration.between(lastAlertTime, now).toMillis();
            long deduplicationWindowMillis = properties.getAlertProcessing().getDeduplicationWindow().toMillis();
            
            if (timeDiffMillis < deduplicationWindowMillis) {
                return true; // This is a duplicate alert
            }
        }
        
        return false;
    }

    private void correlateAlert(AlertEvent alertEvent) {
        String machineId = alertEvent.getMachineId();
        Instant now = Instant.now();
        Instant correlationWindowStart = now.minus(properties.getAlertProcessing().getCorrelationWindow());
        
        // Get recent alerts for the same machine
        List<AlertEntity> recentAlerts = alertRepository.findByMachineIdAndTimestampAfter(
            machineId, correlationWindowStart);
        
        if (!recentAlerts.isEmpty()) {
            // Group alerts by type to identify patterns
            Map<AlertType, List<AlertEntity>> alertsByType = recentAlerts.stream()
                .collect(Collectors.groupingBy(AlertEntity::getType));
            
            // Log correlation information
            for (Map.Entry<AlertType, List<AlertEntity>> entry : alertsByType.entrySet()) {
                if (entry.getValue().size() > 1) {
                    log.info("Correlated {} alerts of type {} for machine {}",
                        entry.getValue().size(), entry.getKey(), machineId);
                }
            }
        }
        
        // Update correlation cache
        correlationCache.computeIfAbsent(machineId, k -> new java.util.ArrayList<>())
            .add(alertEvent.getAlertId());
    }

    private void updateDeduplicationCache(AlertEvent alertEvent) {
        String cacheKey = generateCacheKey(alertEvent);
        alertDeduplicationCache.put(cacheKey, Instant.now());
        
        // Clean up old entries periodically to prevent memory leaks
        cleanUpOldCacheEntries();
    }

    private String generateCacheKey(AlertEvent alertEvent) {
        // Create a unique key based on machineId and alert type for deduplication
        return alertEvent.getMachineId() + "_" + alertEvent.getType();
    }

    private void cleanUpOldCacheEntries() {
        Instant now = Instant.now();
        long deduplicationWindowMillis = properties.getAlertProcessing().getDeduplicationWindow().toMillis();
        
        alertDeduplicationCache.entrySet().removeIf(entry -> {
            long timeDiffMillis = java.time.Duration.between(entry.getValue(), now).toMillis();
            return timeDiffMillis > deduplicationWindowMillis * 2; // Keep for twice the window duration
        });
        
        // Clean up correlation cache as well
        correlationCache.entrySet().removeIf(entry -> {
            // For now, just keep the cache for correlation window duration
            // In a real implementation, we might have more sophisticated cleanup logic
            return false; // Temporarily disable cleanup for correlation cache
        });
    }

    private AlertEntity convertToAlertEntity(AlertEvent alertEvent) {
        return AlertEntity.builder()
                .alertId(alertEvent.getAlertId() != null ? alertEvent.getAlertId() : generateAlertId())
                .machineId(alertEvent.getMachineId())
                .severity(alertEvent.getSeverity())
                .type(alertEvent.getType())
                .timestamp(alertEvent.getTimestamp() != null ? alertEvent.getTimestamp() : Instant.now())
                .description(alertEvent.getDescription())
                .details(alertEvent.getDetails())
                .status(AlertStatus.NEW) // New alerts start as NEW
                .assignedTo(alertEvent.getAssignedTo())
                .resolutionTime(alertEvent.getResolutionTime())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private String generateAlertId() {
        return "ALERT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // New methods required by the controller
    @Transactional(readOnly = true)
    public List<AlertEvent> getAlerts(String machineId, AlertSeverity severity, AlertStatus status) {
        List<AlertEntity> alertEntities;
        
        if (machineId != null && severity != null && status != null) {
            alertEntities = alertRepository.findByMachineIdAndSeverityAndStatus(
                machineId, severity, status);
        } else if (machineId != null && severity != null) {
            alertEntities = alertRepository.findByMachineIdAndSeverity(
                machineId, severity);
        } else if (machineId != null && status != null) {
            alertEntities = alertRepository.findByMachineIdAndStatus(
                machineId, status);
        } else if (severity != null && status != null) {
            alertEntities = alertRepository.findBySeverityAndStatus(
                severity, status);
        } else if (machineId != null) {
            alertEntities = alertRepository.findByMachineId(machineId);
        } else if (severity != null) {
            alertEntities = alertRepository.findBySeverity(severity);
        } else if (status != null) {
            alertEntities = alertRepository.findByStatus(status);
        } else {
            alertEntities = alertRepository.findAll();
        }
        
        return alertEntities.stream()
                .map(this::convertToAlertEvent)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Optional<AlertEvent> getAlertById(String alertId) {
        return alertRepository.findByAlertId(alertId)
                .map(this::convertToAlertEvent);
    }
    
    @Transactional(readOnly = true)
    public List<AlertEvent> getAlertsByMachineId(String machineId) {
        List<AlertEntity> alertEntities = alertRepository.findByMachineId(machineId);
        return alertEntities.stream()
                .map(this::convertToAlertEvent)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public Optional<AlertEvent> acknowledgeAlert(String alertId) {
        Optional<AlertEntity> alertOpt = alertRepository.findByAlertId(alertId);
        if (alertOpt.isPresent()) {
            AlertEntity alert = alertOpt.get();
            alert.setStatus(AlertStatus.ACKNOWLEDGED);
            alert.setUpdatedAt(Instant.now());
            AlertEntity updatedAlert = alertRepository.save(alert);
            return Optional.of(convertToAlertEvent(updatedAlert));
        }
        return Optional.empty();
    }
    
    @Transactional
    public Optional<AlertEvent> resolveAlert(String alertId) {
        Optional<AlertEntity> alertOpt = alertRepository.findByAlertId(alertId);
        if (alertOpt.isPresent()) {
            AlertEntity alert = alertOpt.get();
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolutionTime(Instant.now());
            alert.setUpdatedAt(Instant.now());
            AlertEntity updatedAlert = alertRepository.save(alert);
            return Optional.of(convertToAlertEvent(updatedAlert));
        }
        return Optional.empty();
    }
    
    @Transactional
    public boolean deleteAlert(String alertId) {
        Optional<AlertEntity> alertOpt = alertRepository.findByAlertId(alertId);
        if (alertOpt.isPresent()) {
            alertRepository.delete(alertOpt.get());
            return true;
        }
        return false;
    }
    
    @Transactional(readOnly = true)
    public List<AlertEvent> getAlertsBySeverity(AlertSeverity severity) {
        List<AlertEntity> alertEntities = alertRepository.findBySeverity(severity);
        return alertEntities.stream()
                .map(this::convertToAlertEvent)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertEvent> getAlertsByStatus(AlertStatus status) {
        List<AlertEntity> alertEntities = alertRepository.findByStatus(status);
        return alertEntities.stream()
                .map(this::convertToAlertEvent)
                .collect(Collectors.toList());
    }

    @Transactional
    public AlertEvent updateAlertStatus(String alertId, AlertStatus newStatus) {
        Optional<AlertEntity> alertOpt = alertRepository.findByAlertId(alertId);
        if (alertOpt.isPresent()) {
            AlertEntity alert = alertOpt.get();
            alert.setStatus(newStatus);
            alert.setUpdatedAt(Instant.now());
            AlertEntity updatedAlert = alertRepository.save(alert);
            return convertToAlertEvent(updatedAlert);
        }
        return null;
    }
    
    private AlertEvent convertToAlertEvent(AlertEntity alertEntity) {
        return AlertEvent.builder()
                .alertId(alertEntity.getAlertId())
                .machineId(alertEntity.getMachineId())
                .severity(alertEntity.getSeverity())
                .type(alertEntity.getType())
                .timestamp(alertEntity.getTimestamp())
                .description(alertEntity.getDescription())
                .details(alertEntity.getDetails())
                .status(alertEntity.getStatus())
                .assignedTo(alertEntity.getAssignedTo())
                .resolutionTime(alertEntity.getResolutionTime())
                .build();
    }
}