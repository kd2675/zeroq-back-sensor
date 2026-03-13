package com.zeroq.sensor.database.pub.repository;

import com.zeroq.sensor.database.pub.entity.SensorTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SensorTelemetryRepository extends JpaRepository<SensorTelemetry, Long> {
    Optional<SensorTelemetry> findTopBySensorIdOrderByMeasuredAtDesc(String sensorId);

    Optional<SensorTelemetry> findBySensorIdAndSequenceNoAndMeasuredAt(
            String sensorId,
            Long sequenceNo,
            LocalDateTime measuredAt
    );

    boolean existsBySensorIdAndSequenceNoAndMeasuredAt(
            String sensorId,
            Long sequenceNo,
            LocalDateTime measuredAt
    );

    List<SensorTelemetry> findTop200BySensorIdOrderByMeasuredAtDesc(String sensorId);
}
