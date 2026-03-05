package com.zeroq.sensor.service.command.vo;

import com.zeroq.sensor.database.pub.entity.SensorCommand;
import com.zeroq.sensor.database.pub.entity.SensorCommandStatus;
import com.zeroq.sensor.database.pub.entity.SensorCommandType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SensorCommandResponse {
    private Long id;
    private String sensorId;
    private SensorCommandType commandType;
    private SensorCommandStatus status;
    private String commandPayload;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime sentAt;
    private LocalDateTime acknowledgedAt;
    private String failureReason;
    private String ackPayload;

    public static SensorCommandResponse from(SensorCommand command) {
        return SensorCommandResponse.builder()
                .id(command.getId())
                .sensorId(command.getSensorDevice().getSensorId())
                .commandType(command.getCommandType())
                .status(command.getStatus())
                .commandPayload(command.getCommandPayload())
                .requestedBy(command.getRequestedBy())
                .requestedAt(command.getRequestedAt())
                .sentAt(command.getSentAt())
                .acknowledgedAt(command.getAcknowledgedAt())
                .failureReason(command.getFailureReason())
                .ackPayload(command.getAckPayload())
                .build();
    }
}
