-- Create historical data table for time-series storage
CREATE TABLE historical_data (
    id BIGSERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    temperature DOUBLE PRECISION,
    vibration DOUBLE PRECISION,
    load DOUBLE PRECISION,
    pressure DOUBLE PRECISION,
    rpm DOUBLE PRECISION,
    status VARCHAR(20),
    anomaly_score DOUBLE PRECISION,
    alert_id VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create hypertable for TimescaleDB (this will work even if using regular PostgreSQL)
-- If using TimescaleDB, uncomment the next line:
-- SELECT create_hypertable('historical_data', 'timestamp');

-- Create indexes for efficient querying
CREATE INDEX idx_historical_data_machine_time ON historical_data (machine_id, timestamp DESC);
CREATE INDEX idx_historical_data_timestamp ON historical_data (timestamp DESC);
CREATE INDEX idx_historical_data_machine_id ON historical_data (machine_id);

-- Add composite indexes for common query patterns
CREATE INDEX idx_historical_data_machine_time_temp ON historical_data (machine_id, timestamp DESC, temperature);
CREATE INDEX idx_historical_data_machine_time_vibration ON historical_data (machine_id, timestamp DESC, vibration);
CREATE INDEX idx_historical_data_machine_time_load ON historical_data (machine_id, timestamp DESC, load);

-- Create a continuous aggregate for performance metrics (TimescaleDB feature, but won't break PostgreSQL)
-- This is commented out since it's TimescaleDB specific and might not work with regular PostgreSQL
/*
CREATE MATERIALIZED VIEW performance_metrics_hourly
WITH (timescaledb.continuous) AS
SELECT
    machine_id,
    time_bucket('1 hour', timestamp) AS bucket,
    AVG(temperature) AS avg_temperature,
    AVG(vibration) AS avg_vibration,
    AVG(load) AS avg_load,
    MAX(vibration) AS max_vibration,
    MAX(temperature) AS max_temperature,
    MAX(load) AS max_load,
    COUNT(*) AS data_points
FROM historical_data
GROUP BY machine_id, bucket;
*/