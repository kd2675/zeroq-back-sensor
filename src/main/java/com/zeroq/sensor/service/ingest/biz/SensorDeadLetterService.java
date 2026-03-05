package com.zeroq.sensor.service.ingest.biz;

import com.zeroq.sensor.database.pub.entity.SensorDeadLetter;
import com.zeroq.sensor.database.pub.entity.TelemetrySourceType;
import com.zeroq.sensor.database.pub.repository.SensorDeadLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorDeadLetterService {
    private static final int MAX_SOURCE_TOPIC_LENGTH = 200;
    private static final int MAX_SENSOR_ID_LENGTH = 50;
    private static final int MAX_REASON_CODE_LENGTH = 50;
    private static final int MAX_REASON_MESSAGE_LENGTH = 500;

    private final SensorDeadLetterRepository sensorDeadLetterRepository;

    @Transactional
    public void saveDeadLetter(
            TelemetrySourceType sourceType,
            String sourceTopic,
            String sensorId,
            String payload,
            String reasonCode,
            String reasonMessage
    ) {
        SensorDeadLetter deadLetter = SensorDeadLetter.builder()
                .sourceType(sourceType)
                .sourceTopic(truncate(sourceTopic, MAX_SOURCE_TOPIC_LENGTH))
                .sensorId(truncate(sensorId, MAX_SENSOR_ID_LENGTH))
                .payload(payload == null ? "" : payload)
                .reasonCode(truncate(reasonCode, MAX_REASON_CODE_LENGTH))
                .reasonMessage(truncate(reasonMessage, MAX_REASON_MESSAGE_LENGTH))
                .occurredAt(LocalDateTime.now())
                .build();
        sensorDeadLetterRepository.save(deadLetter);
        log.warn("Dead letter saved: reasonCode={}, sensorId={}, sourceTopic={}", reasonCode, sensorId, sourceTopic);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }
}
