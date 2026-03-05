package com.zeroq.sensor.service.command.vo;

import com.zeroq.sensor.database.pub.entity.SensorCommandStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MqttCommandAckPayload {
    private Long commandId;
    private String sensorId;
    private SensorCommandStatus status;
    private String ackPayload;
    private String failureReason;
    private LocalDateTime acknowledgedAt;
}
