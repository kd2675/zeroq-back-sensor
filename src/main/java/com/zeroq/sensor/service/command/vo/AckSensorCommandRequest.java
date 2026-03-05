package com.zeroq.sensor.service.command.vo;

import com.zeroq.sensor.database.pub.entity.SensorCommandStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AckSensorCommandRequest {
    @NotNull
    private SensorCommandStatus status;

    private String ackPayload;
    private String failureReason;
    private LocalDateTime acknowledgedAt;
}
