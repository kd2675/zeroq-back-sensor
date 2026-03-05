package com.zeroq.sensor.service.device.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstallSensorRequest {
    @NotNull
    private Long placeId;

    private String positionCode;
    private Double occupancyThresholdCm;
    private Double calibrationOffsetCm;
}
