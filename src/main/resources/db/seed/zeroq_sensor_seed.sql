-- Seed data for ZeroQ sensor raw schema
-- Generated: 2026-03-11

USE ZEROQ_SENSOR;

SET @seed_days = 14;
SET @seed_hours = @seed_days * 24;
SET @seed_end_at = TIMESTAMP(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'));
SET @seed_start_at = DATE_SUB(@seed_end_at, INTERVAL (@seed_hours - 1) HOUR);
SET @seat_recent_end_at = TIMESTAMP(DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:00'));
SET @seat_recent_start_at = DATE_SUB(@seat_recent_end_at, INTERVAL 5 MINUTE);

-- 1) 14-day hourly telemetry stream
INSERT INTO sensor_telemetry (
    sensor_id, sequence_no, source_type,
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
    SELECT 'SN-CAFE-101-01' AS sensor_id, 101 AS virtual_place_id, 'GW-SEOUL-01' AS gateway_id, 1 AS sensor_offset, 68.0 AS occupied_base_cm, 168.0 AS vacant_base_cm, 92.0 AS battery_start, 0.030 AS battery_drop_per_hour
    UNION ALL SELECT 'SN-CAFE-101-02', 101, 'GW-SEOUL-01', 2, 70.0, 170.0, 90.0, 0.031
    UNION ALL SELECT 'SN-CAFE-101-03', 101, 'GW-SEOUL-01', 3, 66.0, 166.0, 88.0, 0.033
    UNION ALL SELECT 'SN-COWORK-102-01', 102, 'GW-SEOUL-02', 4, 72.0, 172.0, 91.0, 0.029
    UNION ALL SELECT 'SN-COWORK-102-02', 102, 'GW-SEOUL-02', 5, 74.0, 174.0, 89.0, 0.030
    UNION ALL SELECT 'SN-COWORK-102-03', 102, 'GW-SEOUL-02', 6, 73.0, 173.0, 87.0, 0.032
    UNION ALL SELECT 'SN-LIB-201-01', 201, 'GW-SEOUL-03', 7, 76.0, 176.0, 95.0, 0.027
    UNION ALL SELECT 'SN-LIB-201-02', 201, 'GW-SEOUL-03', 8, 78.0, 178.0, 93.0, 0.028
    UNION ALL SELECT 'SN-MART-301-01', 301, 'GW-SEOUL-04', 9, 74.0, 174.0, 94.0, 0.029
    UNION ALL SELECT 'SN-MART-301-02', 301, 'GW-SEOUL-04', 10, 75.0, 175.0, 92.0, 0.030
)
SELECT
    sp.sensor_id,
    (sp.sensor_offset * 100000) + hs.hour_offset + 1 AS sequence_no,
    'MQTT' AS source_type,
    DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR) AS measured_at,
    DATE_ADD(
        DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR),
        INTERVAL (MOD(hs.hour_offset + sp.sensor_offset, 25) + 5) SECOND
    ) AS received_at,
    ROUND(
        CASE
            WHEN MOD(hs.hour_offset + sp.sensor_offset, 97) = 0 THEN 430.0
            WHEN (
                (sp.virtual_place_id = 101 AND (
                    HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 7 AND 9 OR
                    HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 12 AND 14 OR
                    HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 18 AND 20
                )) OR
                (sp.virtual_place_id = 102 AND HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 10 AND 17) OR
                (sp.virtual_place_id = 201 AND HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 9 AND 21 AND MOD(hs.hour_offset + sp.sensor_offset, 4) <> 0) OR
                (sp.virtual_place_id = 301 AND HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 11 AND 20)
            )
                THEN sp.occupied_base_cm + MOD(hs.hour_offset + sp.sensor_offset, 12)
            ELSE sp.vacant_base_cm + MOD(hs.hour_offset + sp.sensor_offset, 20)
        END,
        2
    ) AS distance_cm,
    CASE
        WHEN MOD(hs.hour_offset + sp.sensor_offset, 97) = 0 THEN 0
        WHEN (
            (sp.virtual_place_id = 101 AND (
                HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 7 AND 9 OR
                HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 12 AND 14 OR
                HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 18 AND 20
            )) OR
            (sp.virtual_place_id = 102 AND HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 10 AND 17) OR
            (sp.virtual_place_id = 201 AND HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 9 AND 21 AND MOD(hs.hour_offset + sp.sensor_offset, 4) <> 0) OR
            (sp.virtual_place_id = 301 AND HOUR(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR)) BETWEEN 11 AND 20)
        ) THEN 1
        ELSE 0
    END AS occupied,
    ROUND(
        CASE
            WHEN MOD(hs.hour_offset + sp.sensor_offset, 97) = 0 THEN 0.55
            WHEN MOD(hs.hour_offset + sp.sensor_offset, 173) = 0 THEN 0.72
            ELSE 0.83 + (MOD(hs.hour_offset + sp.sensor_offset, 14) / 100)
        END,
        2
    ) AS confidence,
    ROUND(21.0 + (MOD(hs.hour_offset + sp.virtual_place_id, 16) * 0.2), 1) AS temperature_c,
    ROUND(37.0 + (MOD(hs.hour_offset + sp.sensor_offset, 20) * 0.8), 1) AS humidity_percent,
    ROUND(GREATEST(6.0, sp.battery_start - (hs.hour_offset * sp.battery_drop_per_hour)), 2) AS battery_percent,
    -45 - MOD(hs.hour_offset + sp.sensor_offset, 25) AS rssi,
    CASE
        WHEN MOD(hs.hour_offset + sp.sensor_offset, 97) = 0 THEN 'OUTLIER'
        WHEN MOD(hs.hour_offset + sp.sensor_offset, 173) = 0 THEN 'STALE'
        ELSE 'VALID'
    END AS quality_status,
    JSON_OBJECT(
        'seedTag', 'WEEKLY_14D_HOURLY',
        'sensorId', sp.sensor_id,
        'virtualPlaceId', sp.virtual_place_id,
        'gatewayId', sp.gateway_id,
        'hourOffset', hs.hour_offset,
        'measuredAt', DATE_FORMAT(DATE_ADD(@seed_start_at, INTERVAL hs.hour_offset HOUR), '%Y-%m-%dT%H:%i:%s')
    ) AS raw_payload,
    NOW() AS create_date,
    NOW() AS update_date
FROM hour_series hs
JOIN sensor_profile sp
ORDER BY sp.sensor_id, measured_at
ON DUPLICATE KEY UPDATE
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

-- 2) Recent 5-minute telemetry samples for five admin-visible sensors
INSERT INTO sensor_telemetry (
    sensor_id, sequence_no, source_type,
    measured_at, received_at, distance_cm, occupied, confidence,
    temperature_c, humidity_percent, battery_percent, rssi,
    quality_status, raw_payload, create_date, update_date
)
WITH RECURSIVE minute_series AS (
    SELECT 0 AS minute_offset
    UNION ALL
    SELECT minute_offset + 1
    FROM minute_series
    WHERE minute_offset + 1 <= 5
),
recent_sensor_profile AS (
    SELECT 'SN-PIR-2041' AS sensor_id, 'GW-ALPHA-01' AS gateway_id, 1 AS sensor_offset, 72.0 AS occupied_cm, 168.0 AS vacant_cm, 88.0 AS battery_start
    UNION ALL SELECT 'SN-ULT-9912', 'GW-ALPHA-01', 2, 79.0, 176.0, 24.0
    UNION ALL SELECT 'SN-PIR-5502', 'GW-ALPHA-01', 3, 74.0, 170.0, 92.0
    UNION ALL SELECT 'SN-ULT-1102', 'GW-BRAVO-07', 4, 420.0, 420.0, 5.0
    UNION ALL SELECT 'SN-PIR-6671', 'GW-BRAVO-07', 5, 84.0, 164.0, 67.0
)
SELECT
    rsp.sensor_id,
    (900000 + (rsp.sensor_offset * 100) + ms.minute_offset) AS sequence_no,
    'MQTT' AS source_type,
    DATE_ADD(@seat_recent_start_at, INTERVAL ms.minute_offset MINUTE) AS measured_at,
    DATE_ADD(DATE_ADD(@seat_recent_start_at, INTERVAL ms.minute_offset MINUTE), INTERVAL (rsp.sensor_offset + 2) SECOND) AS received_at,
    ROUND(
        CASE
            WHEN rsp.sensor_id = 'SN-ULT-1102' THEN 420.0
            WHEN rsp.sensor_id = 'SN-ULT-9912' AND ms.minute_offset IN (1, 2, 5) THEN rsp.occupied_cm + ms.minute_offset
            WHEN rsp.sensor_id = 'SN-PIR-6671' AND ms.minute_offset IN (0, 4, 5) THEN rsp.occupied_cm + ms.minute_offset
            WHEN rsp.sensor_id = 'SN-PIR-2041' THEN rsp.occupied_cm + MOD(ms.minute_offset, 3)
            WHEN rsp.sensor_id = 'SN-PIR-5502' AND ms.minute_offset IN (2, 3) THEN rsp.vacant_cm + ms.minute_offset
            ELSE rsp.vacant_cm + MOD(ms.minute_offset, 4)
        END,
        2
    ) AS distance_cm,
    CASE
        WHEN rsp.sensor_id = 'SN-ULT-1102' THEN 0
        WHEN rsp.sensor_id = 'SN-ULT-9912' AND ms.minute_offset IN (1, 2, 5) THEN 1
        WHEN rsp.sensor_id = 'SN-PIR-6671' AND ms.minute_offset IN (0, 4, 5) THEN 1
        WHEN rsp.sensor_id = 'SN-PIR-2041' THEN 1
        WHEN rsp.sensor_id = 'SN-PIR-5502' AND ms.minute_offset IN (2, 3) THEN 0
        ELSE 0
    END AS occupied,
    ROUND(
        CASE
            WHEN rsp.sensor_id = 'SN-ULT-1102' THEN 0.41
            WHEN rsp.sensor_id = 'SN-ULT-9912' THEN 0.76
            WHEN rsp.sensor_id = 'SN-PIR-6671' THEN 0.82
            ELSE 0.91
        END,
        2
    ) AS confidence,
    ROUND(23.5 + (rsp.sensor_offset * 0.2) + (ms.minute_offset * 0.1), 1) AS temperature_c,
    ROUND(39.0 + (rsp.sensor_offset * 0.5) + (ms.minute_offset * 0.3), 1) AS humidity_percent,
    ROUND(GREATEST(3.0, rsp.battery_start - (ms.minute_offset * 0.3)), 2) AS battery_percent,
    -48 - rsp.sensor_offset - ms.minute_offset AS rssi,
    CASE
        WHEN rsp.sensor_id = 'SN-ULT-1102' THEN 'STALE'
        WHEN rsp.sensor_id = 'SN-ULT-9912' THEN 'LOW_BATTERY'
        ELSE 'VALID'
    END AS quality_status,
    JSON_OBJECT(
        'seedTag', 'RECENT_5M_WINDOW',
        'sensorId', rsp.sensor_id,
        'gatewayId', rsp.gateway_id,
        'minuteOffset', ms.minute_offset,
        'measuredAt', DATE_FORMAT(DATE_ADD(@seat_recent_start_at, INTERVAL ms.minute_offset MINUTE), '%Y-%m-%dT%H:%i:%s')
    ) AS raw_payload,
    NOW() AS create_date,
    NOW() AS update_date
FROM minute_series ms
JOIN recent_sensor_profile rsp
ORDER BY rsp.sensor_id, measured_at
ON DUPLICATE KEY UPDATE
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

-- 3) Sensor heartbeat raw events
INSERT INTO sensor_heartbeat (
    sensor_id, source_type, heartbeat_at, received_at,
    firmware_version, battery_percent, raw_payload,
    create_date, update_date
)
SELECT
    heartbeat_seed.sensor_id,
    'MQTT' AS source_type,
    heartbeat_seed.heartbeat_at,
    DATE_ADD(heartbeat_seed.heartbeat_at, INTERVAL 2 SECOND) AS received_at,
    heartbeat_seed.firmware_version,
    heartbeat_seed.battery_percent,
    JSON_OBJECT(
        'seedTag', 'RECENT_HEARTBEAT',
        'sensorId', heartbeat_seed.sensor_id,
        'gatewayId', heartbeat_seed.gateway_id,
        'heartbeatAt', DATE_FORMAT(heartbeat_seed.heartbeat_at, '%Y-%m-%dT%H:%i:%s')
    ) AS raw_payload,
    NOW() AS create_date,
    NOW() AS update_date
FROM (
    SELECT 'SN-PIR-2041' AS sensor_id, 'GW-ALPHA-01' AS gateway_id, @seat_recent_end_at AS heartbeat_at, '2.4.1' AS firmware_version, 88.0 AS battery_percent
    UNION ALL SELECT 'SN-ULT-9912', 'GW-ALPHA-01', @seat_recent_end_at, '2.4.1', 24.0
    UNION ALL SELECT 'SN-PIR-5502', 'GW-ALPHA-01', @seat_recent_end_at, '2.4.1', 92.0
    UNION ALL SELECT 'SN-ULT-1102', 'GW-BRAVO-07', DATE_SUB(@seat_recent_end_at, INTERVAL 55 MINUTE), '2.3.8', 5.0
    UNION ALL SELECT 'SN-PIR-6671', 'GW-BRAVO-07', @seat_recent_end_at, '2.3.8', 67.0
) heartbeat_seed;

-- 4) Recent commands
INSERT INTO sensor_command (
    sensor_id, command_type, status, command_payload,
    requested_by, requested_at, sent_at, acknowledged_at,
    failure_reason, ack_payload, create_date, update_date
) VALUES
    ('SN-CAFE-101-01', 'SYNC_TIME', 'ACKNOWLEDGED', '{"timezone":"Asia/Seoul"}', 'admin@zeroq.kr', DATE_SUB(@seed_end_at, INTERVAL 3 HOUR), DATE_SUB(@seed_end_at, INTERVAL 2 HOUR), DATE_SUB(@seed_end_at, INTERVAL 2 HOUR), NULL, '{"result":"ok"}', NOW(), NOW()),
    ('SN-COWORK-102-02', 'SET_THRESHOLD', 'SENT', '{"thresholdCm":116}', 'manager@zeroq.kr', DATE_SUB(@seed_end_at, INTERVAL 2 HOUR), DATE_SUB(@seed_end_at, INTERVAL 1 HOUR), NULL, NULL, NULL, NOW(), NOW()),
    ('SN-LIB-201-02', 'REBOOT', 'FAILED', '{"reason":"firmware-check"}', 'admin@zeroq.kr', DATE_SUB(@seed_end_at, INTERVAL 6 HOUR), DATE_SUB(@seed_end_at, INTERVAL 5 HOUR), DATE_SUB(@seed_end_at, INTERVAL 5 HOUR), 'No response from device', '{"code":"TIMEOUT"}', NOW(), NOW());

-- 5) Dead-letter samples
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
        'Sensor not registered in ZEROQ_ADMIN.sensor_registry',
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
