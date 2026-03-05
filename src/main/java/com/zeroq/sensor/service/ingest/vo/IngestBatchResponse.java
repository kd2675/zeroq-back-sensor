package com.zeroq.sensor.service.ingest.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IngestBatchResponse {
    private int telemetrySuccessCount;
    private int telemetryFailureCount;
    private int heartbeatSuccessCount;
    private int heartbeatFailureCount;
}
