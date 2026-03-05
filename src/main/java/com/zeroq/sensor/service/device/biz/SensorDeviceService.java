package com.zeroq.sensor.service.device.biz;

import com.zeroq.sensor.common.config.SensorIngestionProperties;
import com.zeroq.sensor.common.exception.SensorException;
import com.zeroq.sensor.database.pub.entity.SensorDevice;
import com.zeroq.sensor.database.pub.entity.SensorProtocol;
import com.zeroq.sensor.database.pub.entity.SensorStatus;
import com.zeroq.sensor.database.pub.repository.SensorDeviceRepository;
import com.zeroq.sensor.service.device.vo.InstallSensorRequest;
import com.zeroq.sensor.service.device.vo.RegisterSensorRequest;
import com.zeroq.sensor.service.device.vo.SensorDeviceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorDeviceService {
    private final SensorDeviceRepository sensorDeviceRepository;
    private final SensorIngestionProperties ingestionProperties;

    @Transactional
    @CacheEvict(value = {"sensorById", "sensorsByPlace"}, allEntries = true)
    public SensorDeviceResponse registerSensor(RegisterSensorRequest request) {
        SensorDevice existing = sensorDeviceRepository.findBySensorId(request.getSensorId()).orElse(null);

        validateMacAddressUniqueness(existing, request.getMacAddress());

        SensorDevice sensorDevice = existing == null
                ? SensorDevice.builder()
                .sensorId(request.getSensorId())
                .build()
                : existing;

        sensorDevice.setMacAddress(request.getMacAddress());
        sensorDevice.setModel(request.getModel());
        sensorDevice.setFirmwareVersion(request.getFirmwareVersion());
        sensorDevice.setType(request.getType());
        sensorDevice.setProtocol(request.getProtocol() == null ? SensorProtocol.MQTT : request.getProtocol());
        if (request.getMetadataJson() != null) {
            sensorDevice.setMetadataJson(request.getMetadataJson());
        }
        if (request.getOccupancyThresholdCm() != null) {
            sensorDevice.setOccupancyThresholdCm(request.getOccupancyThresholdCm());
        } else if (existing == null) {
            sensorDevice.setOccupancyThresholdCm(ingestionProperties.getDefaultOccupancyThresholdCm());
        }
        if (request.getCalibrationOffsetCm() != null) {
            sensorDevice.setCalibrationOffsetCm(request.getCalibrationOffsetCm());
        }
        if (request.getPlaceId() != null) {
            sensorDevice.install(
                    request.getPlaceId(),
                    request.getPositionCode(),
                    request.getOccupancyThresholdCm(),
                    request.getCalibrationOffsetCm()
            );
        }

        SensorDevice saved = sensorDeviceRepository.save(sensorDevice);
        log.info("Sensor registered: sensorId={}, placeId={}", saved.getSensorId(), saved.getPlaceId());
        return SensorDeviceResponse.from(saved);
    }

    @Transactional
    @CacheEvict(value = {"sensorById", "sensorsByPlace"}, allEntries = true)
    public SensorDeviceResponse installSensor(String sensorId, InstallSensorRequest request) {
        SensorDevice sensorDevice = findEntityBySensorId(sensorId);
        sensorDevice.install(
                request.getPlaceId(),
                request.getPositionCode(),
                request.getOccupancyThresholdCm(),
                request.getCalibrationOffsetCm()
        );
        SensorDevice saved = sensorDeviceRepository.save(sensorDevice);
        log.info("Sensor installed: sensorId={}, placeId={}, position={}",
                saved.getSensorId(),
                saved.getPlaceId(),
                saved.getPositionCode());
        return SensorDeviceResponse.from(saved);
    }

    @Transactional
    @CacheEvict(value = {"sensorById", "sensorsByPlace"}, allEntries = true)
    public SensorDeviceResponse updateSensorStatus(String sensorId, SensorStatus status) {
        SensorDevice sensorDevice = findEntityBySensorId(sensorId);
        sensorDevice.setStatus(status);
        SensorDevice saved = sensorDeviceRepository.save(sensorDevice);
        return SensorDeviceResponse.from(saved);
    }

    @Cacheable(value = "sensorById", key = "#sensorId")
    public SensorDeviceResponse getSensor(String sensorId) {
        return SensorDeviceResponse.from(findEntityBySensorId(sensorId));
    }

    @Cacheable(value = "sensorsByPlace", key = "#placeId == null ? 'ALL' : #placeId")
    public List<SensorDeviceResponse> getSensors(Long placeId) {
        List<SensorDevice> devices = placeId == null
                ? sensorDeviceRepository.findAll()
                : sensorDeviceRepository.findAllByPlaceId(placeId);
        return devices.stream().map(SensorDeviceResponse::from).toList();
    }

    public SensorDevice findEntityBySensorId(String sensorId) {
        return sensorDeviceRepository.findBySensorId(sensorId)
                .orElseThrow(() -> new SensorException.ResourceNotFoundException("SensorDevice", "sensorId", sensorId));
    }

    @Transactional
    @CacheEvict(value = {"sensorById", "sensorsByPlace"}, allEntries = true)
    public SensorDevice saveEntity(SensorDevice sensorDevice) {
        return sensorDeviceRepository.save(sensorDevice);
    }

    private void validateMacAddressUniqueness(SensorDevice existing, String requestedMacAddress) {
        sensorDeviceRepository.findByMacAddress(requestedMacAddress)
                .ifPresent(macOwner -> {
                    if (existing == null || !Objects.equals(existing.getId(), macOwner.getId())) {
                        throw new SensorException.ConflictException("MAC address already in use: " + requestedMacAddress);
                    }
                });
    }
}
