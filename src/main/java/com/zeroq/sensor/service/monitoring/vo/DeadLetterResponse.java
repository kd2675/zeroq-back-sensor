package com.zeroq.sensor.service.monitoring.vo;

import com.zeroq.sensor.database.pub.entity.SensorDeadLetter;
import com.zeroq.sensor.database.pub.entity.TelemetrySourceType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeadLetterResponse {
    private Long id;
    private TelemetrySourceType sourceType;
    private String sourceTopic;
    private String sensorId;
    private String reasonCode;
    private String reasonMessage;
    private LocalDateTime occurredAt;

    public static DeadLetterResponse from(SensorDeadLetter deadLetter) {
        return DeadLetterResponse.builder()
                .id(deadLetter.getId())
                .sourceType(deadLetter.getSourceType())
                .sourceTopic(deadLetter.getSourceTopic())
                .sensorId(deadLetter.getSensorId())
                .reasonCode(deadLetter.getReasonCode())
                .reasonMessage(deadLetter.getReasonMessage())
                .occurredAt(deadLetter.getOccurredAt())
                .build();
    }
}
