package com.zeroq.sensor.database.pub.entity;

import com.zeroq.sensor.common.jpa.CommonDateEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gateway_status_snapshot", indexes = {
        @Index(name = "idx_gateway_status_snapshot_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayStatusSnapshot extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gateway_id", nullable = false, unique = true, length = 50)
    private String gatewayId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GatewayStatus status;

    @Column(name = "last_heartbeat_at", nullable = false)
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "firmware_version", length = 20)
    private String firmwareVersion;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "current_sensor_load", nullable = false)
    @Builder.Default
    private Integer currentSensorLoad = 0;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "packet_loss_percent")
    private Double packetLossPercent;

    @Column(name = "telemetry_pending", nullable = false)
    @Builder.Default
    private Long telemetryPending = 0L;

    @Column(name = "telemetry_failed", nullable = false)
    @Builder.Default
    private Long telemetryFailed = 0L;

    @Column(name = "heartbeat_pending", nullable = false)
    @Builder.Default
    private Long heartbeatPending = 0L;

    @Column(name = "heartbeat_failed", nullable = false)
    @Builder.Default
    private Long heartbeatFailed = 0L;

    @Column(name = "command_dispatch_pending", nullable = false)
    @Builder.Default
    private Long commandDispatchPending = 0L;

    @Column(name = "command_ack_pending", nullable = false)
    @Builder.Default
    private Long commandAckPending = 0L;
}
