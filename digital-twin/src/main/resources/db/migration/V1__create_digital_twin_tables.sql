-- State snapshots for recovery and historical analysis
CREATE TABLE state_snapshots (
    id BIGSERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    state_data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient querying by machine ID and time
CREATE INDEX idx_state_snapshots_machine_time ON state_snapshots (machine_id, snapshot_time DESC);

-- Anomaly history table for historical record of detected anomalies
CREATE TABLE anomaly_history (
    id BIGSERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    anomaly_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    sensor_data JSONB,
    calculated_metrics JSONB,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_anomaly_history_machine_time ON anomaly_history (machine_id, timestamp DESC);
CREATE INDEX idx_anomaly_history_severity ON anomaly_history (severity);

-- Machine configurations table for different machine types
CREATE TABLE machine_configurations (
    id BIGSERIAL PRIMARY KEY,
    machine_type VARCHAR(50) NOT NULL,
    baseline_temp DECIMAL(5,2),
    temp_variation DECIMAL(5,2),
    baseline_vibration DECIMAL(5,2),
    vibration_variation DECIMAL(5,2),
    operational_pattern VARCHAR(20),
    thresholds JSONB, -- Custom thresholds for different sensors
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient querying by machine type
CREATE INDEX idx_machine_configurations_type ON machine_configurations (machine_type);