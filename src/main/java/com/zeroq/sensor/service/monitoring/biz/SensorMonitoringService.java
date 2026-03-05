package com.zeroq.sensor.service.monitoring.biz;

import com.zeroq.sensor.common.exception.SensorException;
import com.zeroq.sensor.database.pub.repository.PlaceOccupancySnapshotRepository;
import com.zeroq.sensor.database.pub.repository.SensorDeadLetterRepository;
import com.zeroq.sensor.database.pub.repository.SensorTelemetryRepository;
import com.zeroq.sensor.service.ingest.biz.SnapshotAggregationService;
import com.zeroq.sensor.service.monitoring.vo.DeadLetterResponse;
import com.zeroq.sensor.service.monitoring.vo.PlaceSnapshotResponse;
import com.zeroq.sensor.service.monitoring.vo.RecentTelemetryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorMonitoringService {
    private final PlaceOccupancySnapshotRepository placeOccupancySnapshotRepository;
    private final SensorTelemetryRepository sensorTelemetryRepository;
    private final SensorDeadLetterRepository sensorDeadLetterRepository;
    private final SnapshotAggregationService snapshotAggregationService;

    @Cacheable(value = "latestSnapshotByPlace", key = "#placeId", condition = "!#recalculate")
    public PlaceSnapshotResponse getPlaceSnapshot(Long placeId, boolean recalculate) {
        if (recalculate) {
            snapshotAggregationService.recalculateSnapshot(placeId);
        }

        return placeOccupancySnapshotRepository.findByPlaceId(placeId)
                .map(PlaceSnapshotResponse::from)
                .orElseThrow(() -> new SensorException.ResourceNotFoundException("PlaceOccupancySnapshot", "placeId", placeId));
    }

    public List<RecentTelemetryResponse> getRecentTelemetry(Long placeId, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 200));
        return sensorTelemetryRepository.findTop200ByPlaceIdOrderByMeasuredAtDesc(placeId).stream()
                .limit(cappedLimit)
                .map(RecentTelemetryResponse::from)
                .toList();
    }

    public List<DeadLetterResponse> getRecentDeadLetters(int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 100));
        return sensorDeadLetterRepository.findTop100ByOrderByOccurredAtDesc().stream()
                .limit(cappedLimit)
                .map(DeadLetterResponse::from)
                .toList();
    }
}
