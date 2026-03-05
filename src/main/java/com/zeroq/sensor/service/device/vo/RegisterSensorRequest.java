package com.zeroq.sensor.service.device.vo;

import com.zeroq.sensor.database.pub.entity.SensorProtocol;
import com.zeroq.sensor.database.pub.entity.SensorType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterSensorRequest {
    @NotBlank
    @Size(max = 50)
    private String sensorId;

    @NotBlank
    @Size(max = 20)
    private String macAddress;

    @NotBlank
    @Size(max = 100)
    private String model;

    @Size(max = 20)
    private String firmwareVersion;

    @NotNull
    private SensorType type;

    @NotNull
    private SensorProtocol protocol = SensorProtocol.MQTT;

    @Positive
    private Long placeId;
    private String positionCode;

    @DecimalMin(value = "0.1")
    private Double occupancyThresholdCm;
    private Double calibrationOffsetCm;
    private String metadataJson;
}
