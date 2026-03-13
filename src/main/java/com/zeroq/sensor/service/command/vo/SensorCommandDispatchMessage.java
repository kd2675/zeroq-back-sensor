package com.zeroq.sensor.service.command.vo;

import com.zeroq.sensor.database.pub.entity.SensorCommand;
import com.zeroq.sensor.database.pub.entity.SensorCommandType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SensorCommandDispatchMessage {
    private Long commandId;
    private String sensorId;
    private SensorCommandType commandType;
    private LocalDateTime requestedAt;
    private String commandPayload;

    public static SensorCommandDispatchMessage from(SensorCommand command) {
        return SensorCommandDispatchMessage.builder()
                .commandId(command.getId())
                .sensorId(command.getSensorId())
                .commandType(command.getCommandType())
                .requestedAt(command.getRequestedAt())
                .commandPayload(command.getCommandPayload())
                .build();
    }
}
