# Database Schema

## Overview

The Industrial Equipment Digital Twin system uses PostgreSQL databases for persistent storage of state snapshots, anomaly records, alerts, and performance metrics. Each service has its own dedicated database to maintain separation of concerns.

## Digital Twin Service Database Schema

### State Snapshots Table
```sql
-- State snapshots for recovery and historical analysis
CREATE TABLE state_snapshots (
    id SERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    snapshot_time TIMESTAMP NOT NULL,
    state_data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient querying by machine ID and time
CREATE INDEX idx_state_snapshots_machine_time ON state_snapshots (machine_id, snapshot_time DESC);
```

### Anomaly History Table
```sql
-- Historical record of detected anomalies
CREATE TABLE anomaly_history (
    id SERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    anomaly_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    sensor_data JSONB,
    calculated_metrics JSONB,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient querying by machine ID and time
CREATE INDEX idx_anomaly_history_machine_time ON anomaly_history (machine_id, timestamp DESC);
CREATE INDEX idx_anomaly_history_severity ON anomaly_history (severity);
```

### Machine Configurations Table
```sql
-- Configuration data for different machine types
CREATE TABLE machine_configurations (
    id SERIAL PRIMARY KEY,
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
```

## Alert/Analytics Service Database Schema

### Alerts Table
```sql
-- Active and historical alerts
CREATE TABLE alerts (
    id SERIAL PRIMARY KEY,
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
```

### Anomaly Records Table
```sql
-- Detailed anomaly records from the digital twin service
CREATE TABLE anomaly_records (
    id SERIAL PRIMARY KEY,
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
```

### Performance Metrics Table
```sql
-- Calculated performance metrics for analytics
CREATE TABLE performance_metrics (
    id SERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    calculation_time TIMESTAMP NOT NULL,
    average_load DECIMAL(5,2),
    average_temperature DECIMAL(5,2),
    average_vibration DECIMAL(5,2),
    uptime_percentage DECIMAL(5,2),
    anomaly_count INTEGER,
    efficiency_rating DECIMAL(5,2),
    ml_model_version VARCHAR(20), -- Future field for ML model tracking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_performance_metrics_machine_time ON performance_metrics (machine_id, calculation_time DESC);
CREATE INDEX idx_performance_metrics_calc_time ON performance_metrics (calculation_time DESC);
```

### Maintenance Reports Table
```sql
-- Generated maintenance reports
CREATE TABLE maintenance_reports (
    id SERIAL PRIMARY KEY,
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
```

### Notification Logs Table
```sql
-- Log of sent notifications
CREATE TABLE notification_logs (
    id SERIAL PRIMARY KEY,
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
```

### ML Models Table (Future Enhancement)
```sql
-- Storage for ML models (for future ML-based anomaly detection)
CREATE TABLE ml_models (
    id SERIAL PRIMARY KEY,
    model_id VARCHAR(50) UNIQUE NOT NULL,
    machine_type VARCHAR(50) NOT NULL,
    model_version VARCHAR(20) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    model_data BYTEA, -- Serialized ML model
    training_data_info JSONB, -- Information about training data
    training_date TIMESTAMP,
    accuracy_score DECIMAL(5,4),
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, ARCHIVED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_ml_models_type_version ON ml_models (machine_type, model_version DESC);
CREATE INDEX idx_ml_models_status ON ml_models (status);
```

## Database Design Principles

### JSONB Usage
- Used for flexible data storage where schema may evolve
- Allows for complex nested structures
- Supports indexing on JSON fields for performance

### Indexing Strategy
- Primary indexes on frequently queried fields
- Composite indexes for multi-field queries
- Time-based indexes for time-series data

### Data Retention
- Automatic cleanup policies for old data
- Separate archival tables for historical data
- Configurable retention periods per table

## Future-Ready Schema Features

### ML Integration Fields
- `ml_features` and `ml_confidence` fields in anomaly records
- `ml_model_version` field in performance metrics
- Dedicated `ml_models` table for model storage

### Extensibility Points
- Generic `details` and `calculated_metrics` JSONB fields
- Flexible configuration storage in `thresholds` field
- Algorithm tracking in `algorithm_used` field

## Performance Considerations

### Partitioning
- Time-based partitioning for large tables (alerts, anomalies)
- Machine-based partitioning for state snapshots

### Connection Pooling
- HikariCP for efficient connection management
- Configurable pool sizes per service

### Query Optimization
- Proper indexing strategy
- Query plan analysis for complex queries
- Regular maintenance tasks (VACUUM, ANALYZE)