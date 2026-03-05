package com.zeroq.sensor.service.device.vo;

import com.zeroq.sensor.database.pub.entity.SensorStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSensorStatusRequest {
    @NotNull
    private SensorStatus status;
}
