-- Seed data for ZeroQ sensor service
-- Range: 14 days (hourly telemetry per active sensor)
-- Generated: 2026-03-06

USE ZEROQ_SENSOR;

SET @seed_days = 14;
SET @seed_hours = @seed_days * 24;
SET @seed_end_at = TIMESTAMP(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'));
SET @seed_start_at = DATE_SUB(@seed_end_at, INTERVAL (@seed_hours - 1) HOUR);

-- 1) Sensor devices (3 spaces, 8 active + 1 maintenance)
INSERT INTO sensor_device (
    sensor_id, mac_address, model, firmware_version, type, protocol, status,
    place_id, position_code, battery_percent, occupancy_threshold_cm, calibration_offset_cm,
    last_heartbeat_at, last_sequence_no, metadata_json, create_date, update_date
) VALUES
    ('SN-CAFE-101-01', 'AA:BB:CC:10:10:01', 'ESP32-WROOM-32D', '1.3.2', 'OCCUPANCY_DETECTION', 'MQTT', 'ACTIVE',      101, 'A1-WINDOW', 91.0, 120.0, 0.0, @seed_end_at, 0, '{"zone":"window","floor":"1"}', NOW(), NOW()),
    ('SN-CAFE-101-02', 'AA:BB:CC:10:10:02', 'ESP32-WROOM-32D', '1.3.2', 'OCCUPANCY_DETECTION', 'MQTT', 'ACTIVE',      101, 'A2-WINDOW', 89.0, 120.0, 0.0, @seed_end_at, 0, '{"zone":"window","floor":"1"}', NOW(), NOW()),
    ('SN-CAFE-101-03', 'AA:BB:CC:10:10:03', 'ESP32-WROOM-32D', '1.3.2', 'OCCUPANCY_DETECTION', 'MQTT', 'ACTIVE',      101, 'A3-COUNTER', 87.0, 120.0, 0.0, @seed_end_at, 0, '{"zone":"counter","floor":"1"}', NOW(), NOW()),
    ('SN-CAFE-101-04', 'AA:BB:CC:10:10:04', 'ESP32-WROOM-32D', '1.3.2', 'OCCUPANCY_DETECTION', 'MQTT', 'MAINTENANCE', 101, 'A4-BACK',   66.0, 120.0, 0.0, DATE_SUB(@seed_end_at, INTERVAL 10 HOUR), 0, '{"zone":"backroom","note":"maintenance"}', NOW(), NOW()),

    ('SN-COWORK-102-01', 'AA:BB:CC:10:20:01', 'ESP32-WROOM-32D', '1.2.9', 'OCCUPANCY_DETECTION', 'MQTT', 'ACTIVE',    102, 'B1-LOUNGE', 90.0, 118.0, -2.0, @seed_end_at, 0, '{"zone":"lounge","floor":"2"}', NOW(), NOW()),
    ('SN-COWORK-102-02', 'AA:BB:CC:10:20:02', 'ESP32-WROOM-32D', '1.2.9', 'OCCUPANCY_DETECTION', 'MQTT', 'ACTIVE',    102, 'B2-ROOM1',  88.0, 118.0, -1.0, @seed_end_at, 0, '{"zone":"meeting-room","floor":"2"}', NOW(), NOW()),
    ('SN-COWORK-102-03', 'AA:BB:CC:10:20:03', 'ESP32-WROOM-32D', '1.2.9', 'OCCUPANCY_DETECTION', 'MQTT', 'ACTIVE',    102, 'B3-ROOM2',  85.0, 118.0, -1.5, @seed_end_at, 0, '{"zone":"meeting-room","floor":"2"}', NOW(), NOW()),

    ('SN-LIB-201-01', 'AA:BB:CC:10:30:01', 'ESP32-S3', '2.0.1', 'OCCUPANCY_DETECTION', 'MQTT', 'ACTIVE',              201, 'C1-DESK',   94.0, 122.0, 1.0, @seed_end_at, 0, '{"zone":"desk-area","floor":"1"}', NOW(), NOW()),
    ('SN-LIB-201-02', 'AA:BB:CC:10:30:02', 'ESP32-S3', '2.0.1', 'OCCUPANCY_DETECTION', 'MQTT', 'ACTIVE',              201, 'C2-DESK',   92.0, 122.0, 1.0, @seed_end_at, 0, '{"zone":"desk-area","floor":"1"}', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    model = VALUES(model),
    firmware_version = VALUES(firmware_version),
    type = VALUES(type),
    protocol = VALUES(protocol),
    status = VALUES(status),
    place_id = VALUES(place_id),
    position_code = VALUES(position_code),
    battery_percent = VALUES(battery_percent),
    occupancy_threshold_cm = VALUES(occupancy_threshold_cm),
    calibration_offset_cm = VALUES(calibration_offset_cm),
    metadata_json = VALUES(metadata_json),
    update_date = NOW();

-- 2) 14-day telemetry stream (hourly)
INSERT INTO sensor_telemetry (
    sensor_device_id, sequence_no, place_id, gateway_id, source_type,
    measured_at, received_at, distance_cm, occupied, confidence,
    temperature_c, humidity_percent, battery_percent, rssi,
    quality_status, raw_payload, create_date, update_date
)
WITH RECURSIVE hour_series AS (
    SELECT 0 AS hour_offset
    UNION ALL
    SELECT hour_offset + 1
    FROM hour_series
    WHERE hour_offset + 1 < @seed_hours
),
sensor_profile AS (
    SELECT 'SN-CAFE-101-01' AS sensor_id, 101 AS place_id, 'GW-SEOUL-01' AS gateway_id, 1 AS sensor_offset, 68.0 AS occupied_base_cm, 168.0 AS vacant_base_cm, 92.0 AS battery_start, 0.030 AS battery_drop_per_hour
    UNION ALL SELECT 'SN-CAFE-101-02', 101, 'GW-SEOUL-01', 2, 70.0, 170.0, 90.0, 0.031
    UNION ALL SELECT 'SN-CAFE-101-03', 101, 'GW-SEOUL-01', 3, 66.0, 166.0, 88.0, 0.033
    UNION ALL SELECT 'SN-COWORK-102-01', 102, 'GW-SEOUL-02', 4, 72.0, 172.0, 91.0, 0.029
    UNION ALL SELECT 'SN-COWORK-102-02', 102, 'GW-SEOUL-02', 5, 74.0, 174.0, 89.0, 0.030
    UNION ALL SELECT 'SN-COWORK-102-03', 102, 'GW-SEOUL-02', 6, 73.0, 173.0, 87.0, 0.032
    UNION ALL SELECT 'SN-LIB-201-01', 201, 'GW-SEOUL-03', 7, 76.0, 176.0, 95.0, 0.027
    UNION ALL SELECT 'SN-LIB-201-02', 201, 'GW-SEOUL-03', 8, 78.0, 178.0, 93.0, 0.028
),
base_stream AS (
    SELECT
        sd.id AS sensor_device_id,
        sp.sensor_id,
        sp.place_id,
        sp.gateway_id,
        sp.sensor_offset,
        sp.occupied_base_cm,
        sp.vacant_base_cm,
        sp.battery_start,
        sp.battery_drop_per_hour,
        hs.hour_offset,
        DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR) AS sample_at
    FROM hour_series hs
    JOIN sensor_profile sp
    JOIN sensor_device sd ON sd.sensor_id = sp.sensor_id
)
SELECT
    bs.sensor_device_id,
    (bs.sensor_offset * 100000) + bs.hour_offset + 1 AS sequence_no,
    bs.place_id,
    bs.gateway_id,
    'MQTT' AS source_type,
    bs.sample_at AS measured_at,
    DATE_ADD(bs.sample_at, INTERVAL (MOD(bs.hour_offset + bs.sensor_offset, 25) + 5) SECOND) AS received_at,
    ROUND(
        CASE
            WHEN MOD(bs.hour_offset + bs.sensor_offset, 97) = 0 THEN 430.0
            WHEN (
                (bs.place_id = 101 AND (
                    HOUR(bs.sample_at) BETWEEN 7 AND 9 OR
                    HOUR(bs.sample_at) BETWEEN 12 AND 14 OR
                    HOUR(bs.sample_at) BETWEEN 18 AND 20
                )) OR
                (bs.place_id = 102 AND HOUR(bs.sample_at) BETWEEN 10 AND 17) OR
                (bs.place_id = 201 AND HOUR(bs.sample_at) BETWEEN 9 AND 21 AND MOD(bs.hour_offset + bs.sensor_offset, 4) <> 0)
            )
                THEN bs.occupied_base_cm + MOD(bs.hour_offset + bs.sensor_offset, 12)
            ELSE bs.vacant_base_cm + MOD(bs.hour_offset + bs.sensor_offset, 20)
        END,
        2
    ) AS distance_cm,
    CASE
        WHEN MOD(bs.hour_offset + bs.sensor_offset, 97) = 0 THEN 0
        WHEN (
            (bs.place_id = 101 AND (
                HOUR(bs.sample_at) BETWEEN 7 AND 9 OR
                HOUR(bs.sample_at) BETWEEN 12 AND 14 OR
                HOUR(bs.sample_at) BETWEEN 18 AND 20
            )) OR
            (bs.place_id = 102 AND HOUR(bs.sample_at) BETWEEN 10 AND 17) OR
            (bs.place_id = 201 AND HOUR(bs.sample_at) BETWEEN 9 AND 21 AND MOD(bs.hour_offset + bs.sensor_offset, 4) <> 0)
        ) THEN 1
        ELSE 0
    END AS occupied,
    ROUND(
        CASE
            WHEN MOD(bs.hour_offset + bs.sensor_offset, 97) = 0 THEN 0.55
            WHEN MOD(bs.hour_offset + bs.sensor_offset, 173) = 0 THEN 0.72
            ELSE 0.83 + (MOD(bs.hour_offset + bs.sensor_offset, 14) / 100)
        END,
        2
    ) AS confidence,
    ROUND(21.0 + (MOD(bs.hour_offset + bs.place_id, 16) * 0.2), 1) AS temperature_c,
    ROUND(37.0 + (MOD(bs.hour_offset + bs.sensor_offset, 20) * 0.8), 1) AS humidity_percent,
    ROUND(GREATEST(6.0, bs.battery_start - (bs.hour_offset * bs.battery_drop_per_hour)), 2) AS battery_percent,
    -45 - MOD(bs.hour_offset + bs.sensor_offset, 25) AS rssi,
    CASE
        WHEN MOD(bs.hour_offset + bs.sensor_offset, 97) = 0 THEN 'OUTLIER'
        WHEN MOD(bs.hour_offset + bs.sensor_offset, 173) = 0 THEN 'STALE'
        ELSE 'VALID'
    END AS quality_status,
    JSON_OBJECT(
        'seedTag', 'WEEKLY_14D_HOURLY',
        'sensorId', bs.sensor_id,
        'placeId', bs.place_id,
        'gatewayId', bs.gateway_id,
        'hourOffset', bs.hour_offset,
        'measuredAt', DATE_FORMAT(bs.sample_at, '%Y-%m-%dT%H:%i:%s')
    ) AS raw_payload,
    NOW() AS create_date,
    NOW() AS update_date
FROM base_stream bs
ORDER BY bs.sensor_device_id, bs.sample_at
ON DUPLICATE KEY UPDATE
    place_id = VALUES(place_id),
    gateway_id = VALUES(gateway_id),
    source_type = VALUES(source_type),
    received_at = VALUES(received_at),
    distance_cm = VALUES(distance_cm),
    occupied = VALUES(occupied),
    confidence = VALUES(confidence),
    temperature_c = VALUES(temperature_c),
    humidity_percent = VALUES(humidity_percent),
    battery_percent = VALUES(battery_percent),
    rssi = VALUES(rssi),
    quality_status = VALUES(quality_status),
    raw_payload = VALUES(raw_payload),
    update_date = NOW();

-- 3) Reflect latest telemetry status to device table
UPDATE sensor_device sd
JOIN (
    SELECT
        sensor_device_id,
        MAX(measured_at) AS last_measured_at,
        MAX(sequence_no) AS last_sequence_no
    FROM sensor_telemetry
    GROUP BY sensor_device_id
) latest ON latest.sensor_device_id = sd.id
LEFT JOIN sensor_telemetry last_row
    ON last_row.sensor_device_id = latest.sensor_device_id
   AND last_row.measured_at = latest.last_measured_at
SET
    sd.last_heartbeat_at = latest.last_measured_at,
    sd.last_sequence_no = latest.last_sequence_no,
    sd.battery_percent = COALESCE(last_row.battery_percent, sd.battery_percent),
    sd.update_date = NOW();

-- 4) Place snapshot derived from latest VALID telemetry per active sensor
INSERT INTO place_occupancy_snapshot (
    place_id,
    occupied_count,
    active_sensor_count,
    occupancy_rate,
    crowd_level,
    last_measured_at,
    last_calculated_at,
    source_window_seconds,
    create_date,
    update_date
)
SELECT
    latest_valid.place_id,
    SUM(latest_valid.occupied) AS occupied_count,
    COUNT(*) AS active_sensor_count,
    ROUND((SUM(latest_valid.occupied) * 100.0) / COUNT(*), 2) AS occupancy_rate,
    CASE
        WHEN ROUND((SUM(latest_valid.occupied) * 100.0) / COUNT(*), 2) <= 10.0 THEN 'EMPTY'
        WHEN ROUND((SUM(latest_valid.occupied) * 100.0) / COUNT(*), 2) <= 35.0 THEN 'LOW'
        WHEN ROUND((SUM(latest_valid.occupied) * 100.0) / COUNT(*), 2) <= 65.0 THEN 'MEDIUM'
        WHEN ROUND((SUM(latest_valid.occupied) * 100.0) / COUNT(*), 2) <= 85.0 THEN 'HIGH'
        ELSE 'FULL'
    END AS crowd_level,
    MAX(latest_valid.measured_at) AS last_measured_at,
    NOW() AS last_calculated_at,
    21600 AS source_window_seconds,
    NOW() AS create_date,
    NOW() AS update_date
FROM (
    SELECT
        st.place_id,
        st.sensor_device_id,
        st.occupied,
        st.measured_at
    FROM sensor_telemetry st
    JOIN (
        SELECT
            sensor_device_id,
            MAX(measured_at) AS max_measured_at
        FROM sensor_telemetry
        WHERE quality_status = 'VALID'
        GROUP BY sensor_device_id
    ) mx
      ON mx.sensor_device_id = st.sensor_device_id
     AND mx.max_measured_at = st.measured_at
    JOIN sensor_device sd
      ON sd.id = st.sensor_device_id
     AND sd.status = 'ACTIVE'
) latest_valid
GROUP BY latest_valid.place_id
ON DUPLICATE KEY UPDATE
    occupied_count = VALUES(occupied_count),
    active_sensor_count = VALUES(active_sensor_count),
    occupancy_rate = VALUES(occupancy_rate),
    crowd_level = VALUES(crowd_level),
    last_measured_at = VALUES(last_measured_at),
    last_calculated_at = VALUES(last_calculated_at),
    source_window_seconds = VALUES(source_window_seconds),
    update_date = NOW();

-- 5) Recent commands
INSERT INTO sensor_command (
    sensor_device_id, command_type, status, command_payload,
    requested_by, requested_at, sent_at, acknowledged_at,
    failure_reason, ack_payload, create_date, update_date
)
SELECT sd.id, 'SYNC_TIME', 'ACKNOWLEDGED', '{"timezone":"Asia/Seoul"}', 'admin@zeroq.kr', DATE_SUB(@seed_end_at, INTERVAL 3 HOUR), DATE_SUB(@seed_end_at, INTERVAL 2 HOUR), DATE_SUB(@seed_end_at, INTERVAL 2 HOUR), NULL, '{"result":"ok"}', NOW(), NOW()
FROM sensor_device sd WHERE sd.sensor_id = 'SN-CAFE-101-01'
UNION ALL
SELECT sd.id, 'SET_THRESHOLD', 'SENT', '{"thresholdCm":116}', 'manager@zeroq.kr', DATE_SUB(@seed_end_at, INTERVAL 2 HOUR), DATE_SUB(@seed_end_at, INTERVAL 1 HOUR), NULL, NULL, NULL, NOW(), NOW()
FROM sensor_device sd WHERE sd.sensor_id = 'SN-COWORK-102-02'
UNION ALL
SELECT sd.id, 'REBOOT', 'FAILED', '{"reason":"firmware-check"}', 'admin@zeroq.kr', DATE_SUB(@seed_end_at, INTERVAL 6 HOUR), DATE_SUB(@seed_end_at, INTERVAL 5 HOUR), DATE_SUB(@seed_end_at, INTERVAL 5 HOUR), 'No response from device', '{"code":"TIMEOUT"}', NOW(), NOW()
FROM sensor_device sd WHERE sd.sensor_id = 'SN-LIB-201-02';

-- 6) Dead-letter samples
INSERT INTO sensor_dead_letter (
    source_type, source_topic, sensor_id, payload,
    reason_code, reason_message, occurred_at, create_date, update_date
) VALUES
    (
        'MQTT',
        'zeroq/sensor/SN-UNKNOWN-009/telemetry',
        'SN-UNKNOWN-009',
        '{"sensorId":"SN-UNKNOWN-009","distanceCm":9999,"gatewayId":"GW-SEOUL-02"}',
        'UNKNOWN_SENSOR',
        'Sensor not registered in sensor_device',
        DATE_SUB(@seed_end_at, INTERVAL 19 HOUR),
        NOW(),
        NOW()
    ),
    (
        'MQTT',
        'zeroq/sensor/SN-CAFE-101-02/command-ack',
        'SN-CAFE-101-02',
        '{"commandId":999999,"status":"ACKNOWLEDGED"}',
        'MQTT_COMMAND_ACK_ERROR',
        'commandId does not exist',
        DATE_SUB(@seed_end_at, INTERVAL 8 HOUR),
        NOW(),
        NOW()
    );
