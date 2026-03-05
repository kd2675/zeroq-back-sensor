package com.zeroq.sensor.service.command.vo;

import com.zeroq.sensor.database.pub.entity.SensorCommandType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSensorCommandRequest {
    @NotBlank
    private String sensorId;

    @NotNull
    private SensorCommandType commandType;

    private String commandPayload;

    @NotBlank
    private String requestedBy;
}
