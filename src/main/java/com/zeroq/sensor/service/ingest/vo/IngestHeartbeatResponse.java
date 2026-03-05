package com.zeroq.sensor.service.ingest.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class IngestHeartbeatResponse {
    private String sensorId;
    private Long placeId;
    private double batteryPercent;
    private LocalDateTime heartbeatAt;
}
