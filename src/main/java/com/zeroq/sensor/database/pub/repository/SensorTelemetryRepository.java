package com.zeroq.sensor.database.pub.repository;

import com.zeroq.sensor.database.pub.entity.SensorDevice;
import com.zeroq.sensor.database.pub.entity.SensorTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SensorTelemetryRepository extends JpaRepository<SensorTelemetry, Long> {
    Optional<SensorTelemetry> findTopBySensorDeviceOrderByMeasuredAtDesc(SensorDevice sensorDevice);

    Optional<SensorTelemetry> findBySensorDeviceAndSequenceNoAndMeasuredAt(
            SensorDevice sensorDevice,
            Long sequenceNo,
            LocalDateTime measuredAt
    );

    boolean existsBySensorDeviceAndSequenceNoAndMeasuredAt(
            SensorDevice sensorDevice,
            Long sequenceNo,
            LocalDateTime measuredAt
    );

    List<SensorTelemetry> findTop200ByPlaceIdOrderByMeasuredAtDesc(Long placeId);

    List<SensorTelemetry> findTop500ByPlaceIdAndMeasuredAtAfterOrderByMeasuredAtDesc(Long placeId, LocalDateTime measuredAt);
}
