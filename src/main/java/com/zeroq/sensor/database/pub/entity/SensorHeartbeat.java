package com.zeroq.sensor.database.pub.entity;

import com.zeroq.sensor.common.jpa.CommonDateEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_heartbeat", indexes = {
        @Index(name = "idx_sensor_heartbeat_sensor_time", columnList = "sensor_id,heartbeat_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorHeartbeat extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false, length = 50)
    private String sensorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private TelemetrySourceType sourceType;

    @Column(name = "heartbeat_at", nullable = false)
    private LocalDateTime heartbeatAt;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "firmware_version", length = 20)
    private String firmwareVersion;

    @Column(name = "battery_percent")
    private Double batteryPercent;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;
}
