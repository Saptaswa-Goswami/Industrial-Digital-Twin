-- Add foreign key constraints where appropriate
-- Note: We're not adding strict FK constraints to maintain flexibility in case referenced records are purged

-- Add additional indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_alerts_composite ON alerts (machine_id, severity, status, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_anomaly_records_composite ON anomaly_records (machine_id, anomaly_type, severity, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_composite ON performance_metrics (machine_id, calculation_time DESC);
CREATE INDEX IF NOT EXISTS idx_maintenance_reports_composite ON maintenance_reports (report_type, generated_time DESC);
-- Add constraints for data integrity
ALTER TABLE alerts ADD CONSTRAINT chk_severity CHECK (severity IN ('CRITICAL', 'WARNING', 'INFO'));
ALTER TABLE alerts ADD CONSTRAINT chk_status CHECK (status IN ('NEW', 'ACKNOWLEDGED', 'RESOLVED', 'IN_PROGRESS'));
ALTER TABLE alerts ADD CONSTRAINT chk_alert_type CHECK (alert_type IN (
    'OVERHEATING',
    'EXCESSIVE_VIBRATION',
    'HIGH_LOAD',
    'PRESSURE_ANOMALY',
    'RPM_ANOMALY',
    'DEGRADATION',
    'PATTERN_ANOMALY',
    'STATISTICAL_OUTLIER',
    'CRITICAL_FAILURE',
    'MAINTENANCE_DUE',
    'TEMPERATURE_HIGH',
    'TEMPERATURE_LOW',
    'VIBRATION_HIGH',
    'VIBRATION_LOW',
    'LOAD_HIGH',
    'LOAD_LOW',
    'PRESSURE_HIGH',
    'PRESSURE_LOW',
    'RPM_HIGH',
    'RPM_LOW'
));

ALTER TABLE anomaly_records ADD CONSTRAINT chk_anomaly_severity CHECK (severity IN ('CRITICAL', 'WARNING', 'INFO'));
ALTER TABLE anomaly_records ADD CONSTRAINT chk_anomaly_type CHECK (anomaly_type IN ('TEMPERATURE_HIGH', 'TEMPERATURE_LOW', 'VIBRATION_HIGH', 'VIBRATION_LOW', 'LOAD_HIGH', 'LOAD_LOW', 'PRESSURE_HIGH', 'PRESSURE_LOW', 'RPM_HIGH', 'RPM_LOW', 'DEGRADATION', 'PATTERN_ANOMALY', 'STATISTICAL_OUTLIER'));

ALTER TABLE maintenance_reports ADD CONSTRAINT chk_report_type CHECK (report_type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'ANNUAL', 'AD_HOC','PERFORMANCE'));

ALTER TABLE notification_logs ADD CONSTRAINT chk_notification_type CHECK (notification_type IN ('EMAIL', 'SMS', 'SLACK', 'MICROSOFT_TEAMS', 'WEBHOOK'));
ALTER TABLE notification_logs ADD CONSTRAINT chk_notification_status CHECK (status IN ('SENT', 'FAILED', 'DELIVERED', 'QUEUED'));


-- Add audit trigger functions for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for automatic timestamp updates
DROP TRIGGER IF EXISTS update_alerts_updated_at ON alerts;
CREATE TRIGGER update_alerts_updated_at BEFORE UPDATE ON alerts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add partitioning for large tables (example for alerts - would be implemented differently in production)
-- Note: This is commented out as actual partitioning would require more complex setup
/*
CREATE TABLE alerts_partition_template (
    LIKE alerts INCLUDING ALL
) INHERITS (alerts);

-- Example of time-based partitioning function (simplified)
CREATE OR REPLACE FUNCTION alerts_insert_trigger_func()
RETURNS TRIGGER AS $$
BEGIN
    -- Insert into appropriate partition based on timestamp
    -- This is a simplified example - real implementation would be more complex
    INSERT INTO alerts VALUES (NEW.*);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER alerts_insert_trigger
    BEFORE INSERT ON alerts
    FOR EACH ROW EXECUTE FUNCTION alerts_insert_trigger_func();
*/