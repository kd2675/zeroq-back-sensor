package com.zeroq.sensor.service.ingest.biz;

import com.zeroq.sensor.common.config.SensorIngestionProperties;
import com.zeroq.sensor.database.pub.entity.PlaceOccupancySnapshot;
import com.zeroq.sensor.database.pub.entity.SensorDevice;
import com.zeroq.sensor.database.pub.entity.SensorStatus;
import com.zeroq.sensor.database.pub.entity.SensorTelemetry;
import com.zeroq.sensor.database.pub.entity.SnapshotCrowdLevel;
import com.zeroq.sensor.database.pub.entity.TelemetryQualityStatus;
import com.zeroq.sensor.database.pub.repository.PlaceOccupancySnapshotRepository;
import com.zeroq.sensor.database.pub.repository.SensorDeviceRepository;
import com.zeroq.sensor.database.pub.repository.SensorTelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SnapshotAggregationService {
    private final SensorTelemetryRepository sensorTelemetryRepository;
    private final SensorDeviceRepository sensorDeviceRepository;
    private final PlaceOccupancySnapshotRepository placeOccupancySnapshotRepository;
    private final SensorIngestionProperties ingestionProperties;

    @Transactional
    @CacheEvict(value = "latestSnapshotByPlace", key = "#placeId")
    public PlaceOccupancySnapshot recalculateSnapshot(Long placeId) {
        List<SensorDevice> activeSensors = sensorDeviceRepository.findAllByPlaceIdAndStatusIn(
                placeId,
                EnumSet.of(SensorStatus.ACTIVE)
        );
        int activeSensorCount = activeSensors.size();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lookback = now.minusSeconds(ingestionProperties.getLookbackSeconds());

        List<SensorTelemetry> latestTelemetry = activeSensors.stream()
                .map(sensorTelemetryRepository::findTopBySensorDeviceOrderByMeasuredAtDesc)
                .flatMap(optional -> optional.stream())
                .filter(telemetry -> telemetry.getMeasuredAt() != null && !telemetry.getMeasuredAt().isBefore(lookback))
                .filter(telemetry -> telemetry.getQualityStatus() == TelemetryQualityStatus.VALID)
                .sorted(Comparator.comparing(SensorTelemetry::getMeasuredAt).reversed())
                .toList();

        int occupiedCount = (int) latestTelemetry.stream()
                .filter(SensorTelemetry::isOccupied)
                .count();

        double occupancyRate = activeSensorCount == 0
                ? 0.0
                : (occupiedCount * 100.0) / activeSensorCount;
        occupancyRate = Math.min(100.0, occupancyRate);

        LocalDateTime lastMeasuredAt = latestTelemetry.stream()
                .map(SensorTelemetry::getMeasuredAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        PlaceOccupancySnapshot snapshot = placeOccupancySnapshotRepository.findByPlaceId(placeId)
                .orElseGet(() -> PlaceOccupancySnapshot.builder()
                        .placeId(placeId)
                        .build());

        snapshot.setActiveSensorCount(activeSensorCount);
        snapshot.setOccupiedCount(occupiedCount);
        snapshot.setOccupancyRate(occupancyRate);
        snapshot.setCrowdLevel(SnapshotCrowdLevel.fromOccupancyRate(occupancyRate));
        snapshot.setLastMeasuredAt(lastMeasuredAt);
        snapshot.setLastCalculatedAt(now);
        snapshot.setSourceWindowSeconds((int) ingestionProperties.getLookbackSeconds());

        PlaceOccupancySnapshot saved = placeOccupancySnapshotRepository.save(snapshot);
        log.debug("Snapshot recalculated: placeId={}, occupied={}, active={}, rate={}",
                placeId,
                occupiedCount,
                activeSensorCount,
                occupancyRate);
        return saved;
    }

    @Transactional
    public void recalculateIfPossible(Long placeId) {
        if (placeId == null) {
            return;
        }
        recalculateSnapshot(placeId);
    }
}
