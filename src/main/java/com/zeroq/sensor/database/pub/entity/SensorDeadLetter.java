package com.zeroq.sensor.database.pub.entity;

import com.zeroq.sensor.common.jpa.CommonDateEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_dead_letter", indexes = {
        @Index(name = "idx_sensor_dead_letter_occurred", columnList = "occurred_at"),
        @Index(name = "idx_sensor_dead_letter_sensor_id", columnList = "sensor_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDeadLetter extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private TelemetrySourceType sourceType;

    @Column(name = "source_topic", length = 200)
    private String sourceTopic;

    @Column(name = "sensor_id", length = 50)
    private String sensorId;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "reason_code", nullable = false, length = 50)
    private String reasonCode;

    @Column(name = "reason_message", nullable = false, length = 500)
    private String reasonMessage;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
