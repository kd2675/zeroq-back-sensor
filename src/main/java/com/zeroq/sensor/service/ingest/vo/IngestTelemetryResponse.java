package com.zeroq.sensor.service.ingest.vo;

import com.zeroq.sensor.database.pub.entity.TelemetryQualityStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class IngestTelemetryResponse {
    private Long telemetryId;
    private String sensorId;
    private TelemetryQualityStatus qualityStatus;
    private boolean occupied;
    private boolean duplicate;
    private LocalDateTime measuredAt;
}
