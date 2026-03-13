package com.zeroq.sensor.database.pub.entity;

import com.zeroq.sensor.common.jpa.CommonDateEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_telemetry", indexes = {
        @Index(name = "idx_sensor_telemetry_sensor_measured", columnList = "sensor_id,measured_at"),
        @Index(name = "idx_sensor_telemetry_quality", columnList = "quality_status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_sensor_telemetry_sensor_sequence_measured", columnNames = {"sensor_id", "sequence_no", "measured_at"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorTelemetry extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false, length = 50)
    private String sensorId;

    @Column(name = "sequence_no")
    private Long sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private TelemetrySourceType sourceType;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "distance_cm")
    private Double distanceCm;

    @Column(name = "occupied", nullable = false)
    private boolean occupied;

    @Column(name = "pad_left_value")
    private Integer padLeftValue;

    @Column(name = "pad_right_value")
    private Integer padRightValue;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "temperature_c")
    private Double temperatureC;

    @Column(name = "humidity_percent")
    private Double humidityPercent;

    @Column(name = "battery_percent")
    private Double batteryPercent;

    @Column(name = "rssi")
    private Integer rssi;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_status", nullable = false, length = 20)
    private TelemetryQualityStatus qualityStatus;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;
}
