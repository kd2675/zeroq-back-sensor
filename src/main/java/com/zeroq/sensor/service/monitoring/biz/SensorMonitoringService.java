package com.zeroq.sensor.service.monitoring.biz;

import com.zeroq.sensor.database.pub.repository.SensorDeadLetterRepository;
import com.zeroq.sensor.database.pub.repository.SensorTelemetryRepository;
import com.zeroq.sensor.service.monitoring.vo.DeadLetterResponse;
import com.zeroq.sensor.service.monitoring.vo.RecentTelemetryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorMonitoringService {
    private final SensorTelemetryRepository sensorTelemetryRepository;
    private final SensorDeadLetterRepository sensorDeadLetterRepository;

    public List<RecentTelemetryResponse> getRecentTelemetryBySensor(String sensorId, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 200));
        return sensorTelemetryRepository.findTop200BySensorIdOrderByMeasuredAtDesc(sensorId).stream()
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
