-- ZeroQ Sensor domain schema
-- Generated: 2026-03-04

DROP SCHEMA IF EXISTS ZEROQ_SENSOR;

CREATE SCHEMA ZEROQ_SENSOR;

USE ZEROQ_SENSOR;

CREATE TABLE IF NOT EXISTS sensor_device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id VARCHAR(50) NOT NULL,
    mac_address VARCHAR(20) NOT NULL,
    model VARCHAR(100) NOT NULL,
    firmware_version VARCHAR(20) NULL,
    type VARCHAR(30) NOT NULL,
    protocol VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    place_id BIGINT NULL,
    position_code VARCHAR(50) NULL,
    battery_percent DOUBLE NOT NULL,
    occupancy_threshold_cm DOUBLE NOT NULL,
    calibration_offset_cm DOUBLE NOT NULL,
    last_heartbeat_at DATETIME NULL,
    last_sequence_no BIGINT NULL,
    metadata_json TEXT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_sensor_device_sensor_id UNIQUE (sensor_id),
    CONSTRAINT uk_sensor_device_mac_address UNIQUE (mac_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sensor_device_sensor_id
    ON sensor_device (sensor_id);

CREATE INDEX idx_sensor_device_place_status
    ON sensor_device (place_id, status);

CREATE TABLE IF NOT EXISTS sensor_telemetry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_device_id BIGINT NOT NULL,
    sequence_no BIGINT NULL,
    place_id BIGINT NULL,
    gateway_id VARCHAR(50) NULL,
    source_type VARCHAR(20) NOT NULL,
    measured_at DATETIME NOT NULL,
    received_at DATETIME NOT NULL,
    distance_cm DOUBLE NOT NULL,
    occupied TINYINT(1) NOT NULL,
    confidence DOUBLE NULL,
    temperature_c DOUBLE NULL,
    humidity_percent DOUBLE NULL,
    battery_percent DOUBLE NULL,
    rssi INT NULL,
    quality_status VARCHAR(20) NOT NULL,
    raw_payload TEXT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_sensor_telemetry_sensor_sequence_measured UNIQUE (sensor_device_id, sequence_no, measured_at),
    CONSTRAINT fk_sensor_telemetry_sensor_device
        FOREIGN KEY (sensor_device_id) REFERENCES sensor_device(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sensor_telemetry_sensor_measured
    ON sensor_telemetry (sensor_device_id, measured_at);

CREATE INDEX idx_sensor_telemetry_place_measured
    ON sensor_telemetry (place_id, measured_at);

CREATE INDEX idx_sensor_telemetry_quality
    ON sensor_telemetry (quality_status);

CREATE TABLE IF NOT EXISTS place_occupancy_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    place_id BIGINT NOT NULL,
    occupied_count INT NOT NULL,
    active_sensor_count INT NOT NULL,
    occupancy_rate DOUBLE NOT NULL,
    crowd_level VARCHAR(20) NOT NULL,
    last_measured_at DATETIME NULL,
    last_calculated_at DATETIME NOT NULL,
    source_window_seconds INT NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_place_occupancy_snapshot_place UNIQUE (place_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_place_occupancy_snapshot_place
    ON place_occupancy_snapshot (place_id);

CREATE INDEX idx_place_occupancy_snapshot_calculated
    ON place_occupancy_snapshot (last_calculated_at);

CREATE TABLE IF NOT EXISTS sensor_command (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_device_id BIGINT NOT NULL,
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
    update_date DATETIME NOT NULL,
    CONSTRAINT fk_sensor_command_sensor_device
        FOREIGN KEY (sensor_device_id) REFERENCES sensor_device(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sensor_command_sensor_status
    ON sensor_command (sensor_device_id, status);

CREATE INDEX idx_sensor_command_requested
    ON sensor_command (requested_at);

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
