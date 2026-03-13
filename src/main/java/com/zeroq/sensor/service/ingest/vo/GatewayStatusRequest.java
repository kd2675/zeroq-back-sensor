package com.zeroq.sensor.service.ingest.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class GatewayStatusRequest {
    @NotBlank
    private String gatewayId;

    @NotBlank
    private String status;

    @NotNull
    private LocalDateTime heartbeatAt;

    private String firmwareVersion;
    private String ipAddress;
    private Integer currentSensorLoad;
    private Integer latencyMs;
    private Double packetLossPercent;
    private Long telemetryPending;
    private Long telemetryFailed;
    private Long heartbeatPending;
    private Long heartbeatFailed;
    private Long commandDispatchPending;
    private Long commandAckPending;
    private String rawPayload;
}
