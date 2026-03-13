package com.zeroq.sensor.service.ingest.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class IngestGatewayStatusResponse {
    private String gatewayId;
    private String status;
    private LocalDateTime heartbeatAt;
}
