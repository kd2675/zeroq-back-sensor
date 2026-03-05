package com.zeroq.sensor.service.ingest.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SensorHeartbeatRequest {
    @NotBlank
    private String sensorId;

    private LocalDateTime heartbeatAt;
    private String firmwareVersion;
    private Double batteryPercent;
    private String gatewayId;
    private Long placeId;
}
