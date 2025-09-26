-- Alerts table
CREATE TABLE alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_id VARCHAR(50) UNIQUE NOT NULL,
    machine_id VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    description TEXT,
    details JSONB,
    status VARCHAR(20) DEFAULT 'NEW',
    assigned_to VARCHAR(100),
    resolution_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_alerts_machine_time ON alerts (machine_id, timestamp DESC);
CREATE INDEX idx_alerts_severity ON alerts (severity);
CREATE INDEX idx_alerts_status ON alerts (status);
CREATE INDEX idx_alerts_assigned ON alerts (assigned_to);

-- Anomaly records table
CREATE TABLE anomaly_records (
    id BIGSERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    anomaly_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    sensor_data JSONB,
    calculated_metrics JSONB,
    ml_features JSONB, -- Future field for ML feature storage
    ml_confidence DECIMAL(5,4), -- Future field for ML confidence score
    algorithm_used VARCHAR(50) DEFAULT 'threshold', -- Track detection method
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_anomaly_records_machine_time ON anomaly_records (machine_id, timestamp DESC);
CREATE INDEX idx_anomaly_records_type ON anomaly_records (anomaly_type);
CREATE INDEX idx_anomaly_records_severity ON anomaly_records (severity);

-- Performance metrics table
CREATE TABLE performance_metrics (
    id BIGSERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    calculation_time TIMESTAMP NOT NULL,
    average_load DOUBLE PRECISION,
    average_temperature DOUBLE PRECISION,
    average_vibration DOUBLE PRECISION,
    uptime_percentage DOUBLE PRECISION,
    anomaly_count INTEGER,
    efficiency_rating DOUBLE PRECISION,
    ml_model_version VARCHAR(20), -- Future field for ML model tracking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_performance_metrics_machine_time ON performance_metrics (machine_id, calculation_time DESC);
CREATE INDEX idx_performance_metrics_calc_time ON performance_metrics (calculation_time DESC);

-- Maintenance reports table
CREATE TABLE maintenance_reports (
    id BIGSERIAL PRIMARY KEY,
    report_id VARCHAR(50) UNIQUE NOT NULL,
    report_type VARCHAR(50) NOT NULL, -- DAILY, WEEKLY, MONTHLY
    generated_time TIMESTAMP NOT NULL,
    machine_id VARCHAR(50),
    content JSONB NOT NULL, -- Report content in JSON format
    metrics_summary JSONB, -- Summary of key metrics
    recommendations JSONB, -- Maintenance recommendations
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_maintenance_reports_time ON maintenance_reports (generated_time DESC);
CREATE INDEX idx_maintenance_reports_type ON maintenance_reports (report_type);
CREATE INDEX idx_maintenance_reports_machine ON maintenance_reports (machine_id);

-- Notification logs table
CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    alert_id VARCHAR(50),
    notification_type VARCHAR(20) NOT NULL, -- EMAIL, SMS, SLACK, etc.
    recipient VARCHAR(100) NOT NULL,
    subject VARCHAR(255),
    content TEXT,
    status VARCHAR(20) NOT NULL, -- SENT, FAILED, DELIVERED
    sent_time TIMESTAMP NOT NULL,
    response_details JSONB, -- Provider-specific response
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_notification_logs_alert ON notification_logs (alert_id);
CREATE INDEX idx_notification_logs_type ON notification_logs (notification_type);
CREATE INDEX idx_notification_logs_status ON notification_logs (status);
CREATE INDEX idx_notification_logs_sent_time ON notification_logs (sent_time DESC);