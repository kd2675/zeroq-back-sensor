-- ZeroQ Sensor raw-data schema
-- Generated: 2026-03-10

DROP SCHEMA IF EXISTS ZEROQ_SENSOR;
CREATE SCHEMA ZEROQ_SENSOR;
USE ZEROQ_SENSOR;

CREATE TABLE IF NOT EXISTS sensor_telemetry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id VARCHAR(50) NOT NULL,
    sequence_no BIGINT NULL,
    source_type VARCHAR(20) NOT NULL,
    measured_at DATETIME NOT NULL,
    received_at DATETIME NOT NULL,
    distance_cm DOUBLE NULL,
    occupied TINYINT(1) NOT NULL,
    pad_left_value INT NULL,
    pad_right_value INT NULL,
    confidence DOUBLE NULL,
    temperature_c DOUBLE NULL,
    humidity_percent DOUBLE NULL,
    battery_percent DOUBLE NULL,
    rssi INT NULL,
    quality_status VARCHAR(20) NOT NULL,
    raw_payload TEXT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_sensor_telemetry_sensor_sequence_measured UNIQUE (sensor_id, sequence_no, measured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sensor_telemetry_sensor_measured
    ON sensor_telemetry (sensor_id, measured_at);

CREATE INDEX idx_sensor_telemetry_quality
    ON sensor_telemetry (quality_status);

CREATE TABLE IF NOT EXISTS sensor_heartbeat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id VARCHAR(50) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    heartbeat_at DATETIME NOT NULL,
    received_at DATETIME NOT NULL,
    firmware_version VARCHAR(20) NULL,
    battery_percent DOUBLE NULL,
    raw_payload TEXT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sensor_heartbeat_sensor_time
    ON sensor_heartbeat (sensor_id, heartbeat_at);

CREATE TABLE IF NOT EXISTS sensor_command (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id VARCHAR(50) NOT NULL,
    command_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    command_payload TEXT NULL,
    requested_by VARCHAR(100) NULL,
    requested_at DATETIME NOT NULL,
    sent_at DATETIME NULL,
    acknowledged_at DATETIME NULL,
    failure_reason VARCHAR(500) NULL,
    ack_payload TEXT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sensor_command_sensor_status
    ON sensor_command (sensor_id, status);

CREATE INDEX idx_sensor_command_requested
    ON sensor_command (requested_at);

CREATE TABLE IF NOT EXISTS gateway_heartbeat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gateway_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    heartbeat_at DATETIME NOT NULL,
    firmware_version VARCHAR(20) NULL,
    ip_address VARCHAR(50) NULL,
    current_sensor_load INT NOT NULL DEFAULT 0,
    latency_ms INT NULL,
    packet_loss_percent DOUBLE NULL,
    telemetry_pending BIGINT NOT NULL DEFAULT 0,
    telemetry_failed BIGINT NOT NULL DEFAULT 0,
    heartbeat_pending BIGINT NOT NULL DEFAULT 0,
    heartbeat_failed BIGINT NOT NULL DEFAULT 0,
    command_dispatch_pending BIGINT NOT NULL DEFAULT 0,
    command_ack_pending BIGINT NOT NULL DEFAULT 0,
    raw_payload TEXT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_gateway_heartbeat_gateway_time
    ON gateway_heartbeat (gateway_id, heartbeat_at);

CREATE TABLE IF NOT EXISTS gateway_status_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gateway_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_heartbeat_at DATETIME NOT NULL,
    firmware_version VARCHAR(20) NULL,
    ip_address VARCHAR(50) NULL,
    current_sensor_load INT NOT NULL DEFAULT 0,
    latency_ms INT NULL,
    packet_loss_percent DOUBLE NULL,
    telemetry_pending BIGINT NOT NULL DEFAULT 0,
    telemetry_failed BIGINT NOT NULL DEFAULT 0,
    heartbeat_pending BIGINT NOT NULL DEFAULT 0,
    heartbeat_failed BIGINT NOT NULL DEFAULT 0,
    command_dispatch_pending BIGINT NOT NULL DEFAULT 0,
    command_ack_pending BIGINT NOT NULL DEFAULT 0,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_gateway_status_snapshot_gateway_id UNIQUE (gateway_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_gateway_status_snapshot_status
    ON gateway_status_snapshot (status);

CREATE TABLE IF NOT EXISTS sensor_dead_letter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_type VARCHAR(20) NOT NULL,
    source_topic VARCHAR(200) NULL,
    sensor_id VARCHAR(50) NULL,
    payload TEXT NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    reason_message VARCHAR(500) NOT NULL,
    occurred_at DATETIME NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sensor_dead_letter_occurred
    ON sensor_dead_letter (occurred_at);

CREATE INDEX idx_sensor_dead_letter_sensor_id
    ON sensor_dead_letter (sensor_id);
