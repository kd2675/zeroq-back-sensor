package com.zeroq.sensor.database.pub.entity;

import com.zeroq.sensor.common.jpa.CommonDateEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_device", indexes = {
        @Index(name = "idx_sensor_device_sensor_id", columnList = "sensor_id"),
        @Index(name = "idx_sensor_device_place_status", columnList = "place_id,status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDevice extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false, unique = true, length = 50)
    private String sensorId;

    @Column(name = "mac_address", nullable = false, unique = true, length = 20)
    private String macAddress;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "firmware_version", length = 20)
    private String firmwareVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SensorType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SensorProtocol protocol = SensorProtocol.MQTT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SensorStatus status = SensorStatus.ACTIVE;

    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "position_code", length = 50)
    private String positionCode;

    @Column(name = "battery_percent", nullable = false)
    @Builder.Default
    private double batteryPercent = 0.0;

    @Column(name = "occupancy_threshold_cm", nullable = false)
    @Builder.Default
    private double occupancyThresholdCm = 120.0;

    @Column(name = "calibration_offset_cm", nullable = false)
    @Builder.Default
    private double calibrationOffsetCm = 0.0;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "last_sequence_no")
    private Long lastSequenceNo;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    public void install(Long placeId, String positionCode, Double thresholdCm, Double calibrationOffsetCm) {
        this.placeId = placeId;
        this.positionCode = positionCode;
        if (thresholdCm != null) {
            this.occupancyThresholdCm = thresholdCm;
        }
        if (calibrationOffsetCm != null) {
            this.calibrationOffsetCm = calibrationOffsetCm;
        }
        this.status = SensorStatus.ACTIVE;
    }

    public void markHeartbeat(LocalDateTime heartbeatAt, Double batteryPercent, String firmwareVersion) {
        if (heartbeatAt != null && (this.lastHeartbeatAt == null || heartbeatAt.isAfter(this.lastHeartbeatAt))) {
            this.lastHeartbeatAt = heartbeatAt;
        }
        if (batteryPercent != null) {
            this.batteryPercent = batteryPercent;
        }
        if (firmwareVersion != null && !firmwareVersion.isBlank()) {
            this.firmwareVersion = firmwareVersion;
        }
    }

    public void markTelemetry(Long sequenceNo, Double batteryPercent, LocalDateTime measuredAt) {
        if (sequenceNo != null) {
            this.lastSequenceNo = sequenceNo;
        }
        if (batteryPercent != null) {
            this.batteryPercent = batteryPercent;
        }
        if (measuredAt != null && (this.lastHeartbeatAt == null || measuredAt.isAfter(this.lastHeartbeatAt))) {
            this.lastHeartbeatAt = measuredAt;
        }
    }
}
