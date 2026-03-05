package com.zeroq.sensor.service.monitoring.vo;

import com.zeroq.sensor.database.pub.entity.SensorTelemetry;
import com.zeroq.sensor.database.pub.entity.TelemetryQualityStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecentTelemetryResponse {
    private Long telemetryId;
    private String sensorId;
    private Long placeId;
    private double distanceCm;
    private boolean occupied;
    private TelemetryQualityStatus qualityStatus;
    private LocalDateTime measuredAt;
    private LocalDateTime receivedAt;
    private Double batteryPercent;
    private Double confidence;

    public static RecentTelemetryResponse from(SensorTelemetry telemetry) {
        return RecentTelemetryResponse.builder()
                .telemetryId(telemetry.getId())
                .sensorId(telemetry.getSensorDevice().getSensorId())
                .placeId(telemetry.getPlaceId())
                .distanceCm(telemetry.getDistanceCm())
                .occupied(telemetry.isOccupied())
                .qualityStatus(telemetry.getQualityStatus())
                .measuredAt(telemetry.getMeasuredAt())
                .receivedAt(telemetry.getReceivedAt())
                .batteryPercent(telemetry.getBatteryPercent())
                .confidence(telemetry.getConfidence())
                .build();
    }
}
