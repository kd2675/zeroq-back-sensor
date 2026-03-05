package com.zeroq.sensor.service.device.vo;

import com.zeroq.sensor.database.pub.entity.SensorDevice;
import com.zeroq.sensor.database.pub.entity.SensorProtocol;
import com.zeroq.sensor.database.pub.entity.SensorStatus;
import com.zeroq.sensor.database.pub.entity.SensorType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SensorDeviceResponse {
    private Long id;
    private String sensorId;
    private String macAddress;
    private String model;
    private String firmwareVersion;
    private SensorType type;
    private SensorProtocol protocol;
    private SensorStatus status;
    private Long placeId;
    private String positionCode;
    private double batteryPercent;
    private double occupancyThresholdCm;
    private double calibrationOffsetCm;
    private LocalDateTime lastHeartbeatAt;
    private Long lastSequenceNo;
    private String metadataJson;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

    public static SensorDeviceResponse from(SensorDevice device) {
        return SensorDeviceResponse.builder()
                .id(device.getId())
                .sensorId(device.getSensorId())
                .macAddress(device.getMacAddress())
                .model(device.getModel())
                .firmwareVersion(device.getFirmwareVersion())
                .type(device.getType())
                .protocol(device.getProtocol())
                .status(device.getStatus())
                .placeId(device.getPlaceId())
                .positionCode(device.getPositionCode())
                .batteryPercent(device.getBatteryPercent())
                .occupancyThresholdCm(device.getOccupancyThresholdCm())
                .calibrationOffsetCm(device.getCalibrationOffsetCm())
                .lastHeartbeatAt(device.getLastHeartbeatAt())
                .lastSequenceNo(device.getLastSequenceNo())
                .metadataJson(device.getMetadataJson())
                .createDate(device.getCreateDate())
                .updateDate(device.getUpdateDate())
                .build();
    }
}
