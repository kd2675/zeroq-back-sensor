-- Seed data for ZeroQ sensor service
-- Generated: 2026-03-04

USE ZEROQ_SENSOR;

INSERT INTO sensor_device (
    sensor_id, mac_address, model, firmware_version, type, protocol, status,
    place_id, position_code, battery_percent, occupancy_threshold_cm, calibration_offset_cm,
    last_heartbeat_at, last_sequence_no, metadata_json, create_date, update_date
) VALUES
    (
        'SN-CAFE-0001',
        'AA:BB:CC:00:00:01',
        'ESP32-WROOM-32D',
        '1.1.0',
        'OCCUPANCY_DETECTION',
        'MQTT',
        'ACTIVE',
        101,
        'TABLE-A1',
        86.0,
        120.0,
        0.0,
        NOW(),
        112,
        '{"zone":"window"}',
        NOW(),
        NOW()
    ),
    (
        'SN-CAFE-0002',
        'AA:BB:CC:00:00:02',
        'ESP32-WROOM-32D',
        '1.1.0',
        'OCCUPANCY_DETECTION',
        'MQTT',
        'ACTIVE',
        101,
        'TABLE-A2',
        78.0,
        120.0,
        0.0,
        NOW(),
        98,
        '{"zone":"counter"}',
        NOW(),
        NOW()
    ),
    (
        'SN-CAFE-0003',
        'AA:BB:CC:00:00:03',
        'ESP32-WROOM-32D',
        '1.0.8',
        'OCCUPANCY_DETECTION',
        'MQTT',
        'INACTIVE',
        102,
        'TABLE-B1',
        21.0,
        120.0,
        0.0,
        NOW(),
        77,
        '{"zone":"hall"}',
        NOW(),
        NOW()
    );

INSERT INTO sensor_telemetry (
    sensor_device_id, sequence_no, place_id, gateway_id, source_type,
    measured_at, received_at, distance_cm, occupied, confidence,
    temperature_c, humidity_percent, battery_percent, rssi,
    quality_status, raw_payload, create_date, update_date
) VALUES
    (1, 113, 101, 'GW-SEOUL-01', 'MQTT', NOW(), NOW(), 95.2, 1, 0.98, 23.1, 40.2, 85.0, -61, 'VALID', '{"distanceCm":95.2}', NOW(), NOW()),
    (2, 99, 101, 'GW-SEOUL-01', 'MQTT', NOW(), NOW(), 170.4, 0, 0.97, 23.4, 39.8, 77.0, -58, 'VALID', '{"distanceCm":170.4}', NOW(), NOW()),
    (3, 78, 102, 'GW-SEOUL-02', 'MQTT', NOW(), NOW(), 88.1, 1, 0.82, 24.0, 41.3, 20.0, -70, 'STALE', '{"distanceCm":88.1}', NOW(), NOW());

INSERT INTO place_occupancy_snapshot (
    place_id, occupied_count, active_sensor_count, occupancy_rate, crowd_level,
    last_measured_at, last_calculated_at, source_window_seconds, create_date, update_date
) VALUES
    (101, 1, 2, 50.0, 'MEDIUM', NOW(), NOW(), 300, NOW(), NOW()),
    (102, 1, 0, 0.0, 'EMPTY', NOW(), NOW(), 300, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    occupied_count = VALUES(occupied_count),
    active_sensor_count = VALUES(active_sensor_count),
    occupancy_rate = VALUES(occupancy_rate),
    crowd_level = VALUES(crowd_level),
    last_measured_at = VALUES(last_measured_at),
    last_calculated_at = VALUES(last_calculated_at),
    source_window_seconds = VALUES(source_window_seconds),
    update_date = NOW();

INSERT INTO sensor_command (
    sensor_device_id, command_type, status, command_payload,
    requested_by, requested_at, sent_at, acknowledged_at,
    failure_reason, ack_payload, create_date, update_date
) VALUES
    (1, 'SYNC_TIME', 'PENDING', '{"timezone":"Asia/Seoul"}', 'admin@zeroq.kr', NOW(), NULL, NULL, NULL, NULL, NOW(), NOW()),
    (2, 'SET_THRESHOLD', 'ACKNOWLEDGED', '{"thresholdCm":115}', 'manager@zeroq.kr', NOW(), NOW(), NOW(), NULL, '{"result":"ok"}', NOW(), NOW());

INSERT INTO sensor_dead_letter (
    source_type, source_topic, sensor_id, payload,
    reason_code, reason_message, occurred_at, create_date, update_date
) VALUES
    (
        'MQTT',
        'zeroq/sensor/SN-UNKNOWN/telemetry',
        'SN-UNKNOWN',
        '{"sensorId":"SN-UNKNOWN","distanceCm":9999}',
        'OUT_OF_RANGE',
        'distanceCm exceeds max distance threshold',
        NOW(),
        NOW(),
        NOW()
    );
