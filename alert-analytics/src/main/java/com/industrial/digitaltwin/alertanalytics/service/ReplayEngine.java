package com.industrial.digitaltwin.alertanalytics.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.industrial.digitaltwin.alertanalytics.model.MachineTelemetry;
import com.industrial.digitaltwin.alertanalytics.websocket.ReplayWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReplayEngine {

    private final HistoricalStateService historicalStateService;
    private final WebSocketBroadcastService webSocketBroadcastService;
    private final ReplayWebSocketHandler replayWebSocketHandler;
    
    public ReplayEngine(HistoricalStateService historicalStateService,
                       WebSocketBroadcastService webSocketBroadcastService,
                       ReplayWebSocketHandler replayWebSocketHandler) {
        this.historicalStateService = historicalStateService;
        this.webSocketBroadcastService = webSocketBroadcastService;
        this.replayWebSocketHandler = replayWebSocketHandler;
    }
    
    // Store active replay sessions
    private final ConcurrentHashMap<String, ReplaySession> activeReplays = new ConcurrentHashMap<>();
    
    // Scheduled executor for cleanup tasks
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * Starts a historical replay session for a specific machine
     */
    public String startReplay(String machineId, Instant startTime, Instant endTime, double speedMultiplier) {
        String replayId = generateReplayId();
        ReplaySession session = new ReplaySession(replayId, machineId, startTime, endTime, speedMultiplier);
        
        activeReplays.put(replayId, session);
        
        // Start the replay in a separate thread
        CompletableFuture.runAsync(() -> executeReplay(session));
        
        log.info("Started replay session: {} for machine: {} from: {} to: {} at {}x speed", 
            replayId, machineId, startTime, endTime, speedMultiplier);
            
        return replayId;
    }
    
    /**
     * Pauses an active replay session
     */
    public boolean pauseReplay(String replayId) {
        ReplaySession session = activeReplays.get(replayId);
        if (session != null) {
            session.setPaused(true);
            log.info("Paused replay session: {}", replayId);
            return true;
        }
        return false;
    }
    
    /**
     * Resumes a paused replay session
     */
    public boolean resumeReplay(String replayId) {
        ReplaySession session = activeReplays.get(replayId);
        if (session != null) {
            session.setPaused(false);
            log.info("Resumed replay session: {}", replayId);
            return true;
        }
        return false;
    }
    
    /**
     * Stops an active replay session
     */
    public boolean stopReplay(String replayId) {
        ReplaySession session = activeReplays.remove(replayId);
        if (session != null) {
            session.setStopped(true);
            log.info("Stopped replay session: {}", replayId);
            return true;
        }
        return false;
    }
    
    /**
     * Seeks to a specific time in the replay
     */
    public boolean seekReplay(String replayId, Instant targetTime) {
        ReplaySession session = activeReplays.get(replayId);
        if (session != null) {
            session.setCurrentTime(targetTime);
            log.info("Seeked replay session: {} to time: {}", replayId, targetTime);
            return true;
        }
        return false;
    }
    
    /**
     * Gets the status of a replay session
     */
    public ReplayStatus getReplayStatus(String replayId) {
        ReplaySession session = activeReplays.get(replayId);
        if (session != null) {
            return new ReplayStatus(
                replayId,
                session.getMachineId(),
                session.getCurrentTime(),
                session.getEndTime(),
                session.isPaused(),
                session.isStopped(),
                session.getSpeedMultiplier(),
                session.getStatus()
            );
        }
        return null;
    }
    
    /**
     * Gets the available time boundaries for historical data for a specific machine
     */
    public java.util.Optional<TimeBoundaries> getTimeBoundaries(String machineId) {
        // Get the minimum and maximum timestamps for the machine from the historical data repository
        java.util.Optional<java.time.Instant> minTime = historicalStateService.getMinTimestampForMachine(machineId);
        java.util.Optional<java.time.Instant> maxTime = historicalStateService.getMaxTimestampForMachine(machineId);
        
        if (minTime.isPresent() && maxTime.isPresent()) {
            return java.util.Optional.of(new TimeBoundaries(minTime.get(), maxTime.get()));
        }
        return java.util.Optional.empty();
    }
    
    @lombok.Getter
    public static class TimeBoundaries {
        private final java.time.Instant minTime;
        private final java.time.Instant maxTime;
        
        public TimeBoundaries(java.time.Instant minTime, java.time.Instant maxTime) {
            this.minTime = minTime;
            this.maxTime = maxTime;
        }
    }
    
    private void executeReplay(ReplaySession session) {
        try {
            log.debug("Starting replay execution for session: {}", session.getReplayId());
            
            // Get the timeline of historical states
            List<MachineTelemetry> timeline = historicalStateService.getHistoricalStateTimeline(
                session.getMachineId(),
                session.getStartTime(),
                session.getEndTime()
            );
            
            if (timeline.isEmpty()) {
                log.warn("No historical data found for replay: {} in time range", session.getReplayId());
                // Update the session status to indicate no data is available
                session.setStatus(ReplayStatusType.NO_DATA_AVAILABLE);
                // Don't remove the session from activeReplays - keep it for status checks
                // Schedule automatic cleanup after a timeout period
                scheduleSessionCleanup(session.getReplayId());
                return;
            }
            
            // Update status to running if we have data
            session.setStatus(ReplayStatusType.RUNNING);
            
            // Process each state in the timeline
            Instant lastStateTime = null;
            for (MachineTelemetry state : timeline) {
                // Check if replay is paused or stopped
                if (session.isPaused()) {
                    while (session.isPaused() && !session.isStopped()) {
                        Thread.sleep(100); // Wait while paused
                    }
                }
                
                if (session.isStopped()) {
                    log.debug("Replay session: {} was stopped", session.getReplayId());
                    session.setStatus(ReplayStatusType.STOPPED);
                    break;
                }
                
                // Broadcast the current state via replay-specific WebSocket
                replayWebSocketHandler.broadcastHistoricalState(state);
                
                // Calculate delay based on speed multiplier and time difference
                if (lastStateTime != null) {
                    long timeDiffMillis = java.time.Duration.between(lastStateTime, state.getTimestamp()).toMillis();
                    long delay = (long) (timeDiffMillis / session.getSpeedMultiplier());
                    
                    // Only delay if it's a reasonable time difference (not zero or negative)
                    if (delay > 0) {
                        Thread.sleep(Math.min(delay, 1000)); // Cap delay at 1 second to avoid long waits
                    }
                }
                
                lastStateTime = state.getTimestamp();
                session.setCurrentTime(state.getTimestamp());
            }
            
            if (!session.isStopped()) {
                log.info("Completed replay session: {}", session.getReplayId());
                session.setStatus(ReplayStatusType.COMPLETED);
            }
            
        } catch (InterruptedException e) {
            log.warn("Replay session: {} was interrupted", session.getReplayId());
            session.setStatus(ReplayStatusType.STOPPED);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during replay execution for session: {}", session.getReplayId(), e);
            session.setStatus(ReplayStatusType.STOPPED);
        } finally {
            // Only remove the session if it's not in NO_DATA_AVAILABLE status
            // Sessions with NO_DATA_AVAILABLE status are handled by the cleanup scheduler
            if (session.getStatus() != ReplayStatusType.NO_DATA_AVAILABLE) {
                activeReplays.remove(session.getReplayId());
            }
        }
    }
    
    private void scheduleSessionCleanup(String replayId) {
        // Schedule the session for automatic cleanup after 10 minutes
        scheduler.schedule(() -> {
            activeReplays.remove(replayId);
            log.debug("Cleaned up replay session {} after timeout due to no data", replayId);
        }, 10, TimeUnit.MINUTES);
    }
    
    private String generateReplayId() {
        return "REPLAY_" + System.currentTimeMillis() + "_" + 
               Thread.currentThread().getId() + "_" + 
               (int)(Math.random() * 1000);
    }
    
    // Inner classes for replay session management
    public enum ReplayStatusType {
        RUNNING, PAUSED, STOPPED, NO_DATA_AVAILABLE, COMPLETED
    }

    private static class ReplaySession {
        private final String replayId;
        private final String machineId;
        private final Instant startTime;
        private final Instant endTime;
        private final double speedMultiplier;
        private volatile Instant currentTime;
        private volatile boolean paused = false;
        private volatile boolean stopped = false;
        private volatile ReplayStatusType status = ReplayStatusType.RUNNING;
        private volatile Instant createdTime = Instant.now();
        
        public ReplaySession(String replayId, String machineId, Instant startTime,
                           Instant endTime, double speedMultiplier) {
            this.replayId = replayId;
            this.machineId = machineId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.speedMultiplier = speedMultiplier;
            this.currentTime = startTime;
        }
        
        // Getters and setters
        public String getReplayId() { return replayId; }
        public String getMachineId() { return machineId; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public double getSpeedMultiplier() { return speedMultiplier; }
        public Instant getCurrentTime() { return currentTime; }
        public void setCurrentTime(Instant currentTime) { this.currentTime = currentTime; }
        public boolean isPaused() { return paused; }
        public void setPaused(boolean paused) {
            this.paused = paused;
            if (paused) {
                this.status = ReplayStatusType.PAUSED;
            } else if (!stopped) {
                this.status = ReplayStatusType.RUNNING;
            }
        }
        public boolean isStopped() { return stopped; }
        public void setStopped(boolean stopped) {
            this.stopped = stopped;
            if (stopped) {
                this.status = ReplayStatusType.STOPPED;
            }
        }
        public ReplayStatusType getStatus() { return status; }
        public void setStatus(ReplayStatusType status) { this.status = status; }
        public Instant getCreatedTime() { return createdTime; }
    }
    
    @lombok.Getter
    public static class ReplayStatus {
        private final String replayId;
        private final String machineId;
        private final Instant currentTime;
        private final Instant endTime;
        private final boolean paused;
        private final boolean stopped;
        private final double speedMultiplier;
        private final ReplayStatusType status;
        
        public ReplayStatus(String replayId, String machineId, Instant currentTime,
                          Instant endTime, boolean paused, boolean stopped, double speedMultiplier) {
            this.replayId = replayId;
            this.machineId = machineId;
            this.currentTime = currentTime;
            this.endTime = endTime;
            this.paused = paused;
            this.stopped = stopped;
            this.speedMultiplier = speedMultiplier;
            this.status = ReplayStatusType.RUNNING; // Default status
        }
        
        public ReplayStatus(String replayId, String machineId, Instant currentTime,
                          Instant endTime, boolean paused, boolean stopped, double speedMultiplier, ReplayStatusType status) {
            this.replayId = replayId;
            this.machineId = machineId;
            this.currentTime = currentTime;
            this.endTime = endTime;
            this.paused = paused;
            this.stopped = stopped;
            this.speedMultiplier = speedMultiplier;
            this.status = status;
        }
        
        public boolean isActive() { return !stopped && !paused; }
    }
}