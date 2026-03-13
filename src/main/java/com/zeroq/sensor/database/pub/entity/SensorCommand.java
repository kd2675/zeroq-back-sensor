package com.zeroq.sensor.database.pub.entity;

import com.zeroq.sensor.common.jpa.CommonDateEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_command", indexes = {
        @Index(name = "idx_sensor_command_sensor_status", columnList = "sensor_id,status"),
        @Index(name = "idx_sensor_command_requested", columnList = "requested_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorCommand extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false, length = 50)
    private String sensorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 30)
    private SensorCommandType commandType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SensorCommandStatus status = SensorCommandStatus.PENDING;

    @Column(name = "command_payload", columnDefinition = "TEXT")
    private String commandPayload;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "ack_payload", columnDefinition = "TEXT")
    private String ackPayload;

    public void markSent(LocalDateTime sentAt) {
        this.status = SensorCommandStatus.SENT;
        this.sentAt = sentAt;
    }

    public void markAcknowledged(LocalDateTime acknowledgedAt, String ackPayload) {
        this.status = SensorCommandStatus.ACKNOWLEDGED;
        this.acknowledgedAt = acknowledgedAt;
        this.ackPayload = ackPayload;
        this.failureReason = null;
    }

    public void markFailed(LocalDateTime acknowledgedAt, String failureReason, String ackPayload) {
        this.status = SensorCommandStatus.FAILED;
        this.acknowledgedAt = acknowledgedAt;
        this.failureReason = failureReason;
        this.ackPayload = ackPayload;
    }
}
