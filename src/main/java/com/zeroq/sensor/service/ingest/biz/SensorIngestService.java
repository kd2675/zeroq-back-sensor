package com.zeroq.sensor.service.ingest.biz;

import com.zeroq.sensor.common.config.SensorIngestionProperties;
import com.zeroq.sensor.common.exception.SensorException;
import com.zeroq.sensor.database.pub.entity.*;
import com.zeroq.sensor.database.pub.repository.SensorHeartbeatRepository;
import com.zeroq.sensor.database.pub.repository.SensorTelemetryRepository;
import com.zeroq.sensor.service.ingest.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional(readOnly = true)
public class SensorIngestService {
    private final SensorTelemetryRepository sensorTelemetryRepository;
    private final SensorHeartbeatRepository sensorHeartbeatRepository;
    private final SensorDeadLetterService sensorDeadLetterService;
    private final SensorIngestionProperties ingestionProperties;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public SensorIngestService(
            SensorTelemetryRepository sensorTelemetryRepository,
            SensorHeartbeatRepository sensorHeartbeatRepository,
            SensorDeadLetterService sensorDeadLetterService,
            SensorIngestionProperties ingestionProperties,
            PlatformTransactionManager transactionManager
    ) {
        this.sensorTelemetryRepository = sensorTelemetryRepository;
        this.sensorHeartbeatRepository = sensorHeartbeatRepository;
        this.sensorDeadLetterService = sensorDeadLetterService;
        this.ingestionProperties = ingestionProperties;

        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public IngestTelemetryResponse ingestTelemetry(
            SensorTelemetryRequest request,
            TelemetrySourceType sourceType,
            String sourceTopic
    ) {
        String sensorId = request.getSensorId().trim();
        TelemetryQualityStatus qualityStatus = determineQuality(sensorId, request);
        if (qualityStatus == TelemetryQualityStatus.DUPLICATE) {
            return buildDuplicateResponse(sensorId, request);
        }

        Boolean explicitOccupied = request.getOccupied();
        Double distanceCm = request.getDistanceCm();
        boolean occupied;
        if (explicitOccupied != null) {
            occupied = explicitOccupied;
        } else if (distanceCm != null) {
            occupied = distanceCm <= ingestionProperties.getDefaultOccupancyThresholdCm();
        } else {
            throw new SensorException.ValidationException("Either distanceCm or occupied is required");
        }

        SensorTelemetry telemetry = SensorTelemetry.builder()
                .sensorId(sensorId)
                .sequenceNo(request.getSequenceNo())
                .sourceType(sourceType)
                .measuredAt(request.getMeasuredAt())
                .receivedAt(LocalDateTime.now())
                .distanceCm(distanceCm)
                .occupied(occupied)
                .padLeftValue(request.getPadLeftValue())
                .padRightValue(request.getPadRightValue())
                .confidence(request.getConfidence())
                .temperatureC(request.getTemperatureC())
                .humidityPercent(request.getHumidityPercent())
                .batteryPercent(request.getBatteryPercent())
                .rssi(request.getRssi())
                .qualityStatus(qualityStatus)
                .rawPayload(request.getRawPayload())
                .build();

        SensorTelemetry saved = saveTelemetryWithRaceHandling(sensorId, request, telemetry);

        return IngestTelemetryResponse.builder()
                .telemetryId(saved.getId())
                .sensorId(sensorId)
                .qualityStatus(saved.getQualityStatus())
                .occupied(saved.isOccupied())
                .duplicate(false)
                .measuredAt(saved.getMeasuredAt())
                .build();
    }

    @Transactional
    public IngestHeartbeatResponse ingestHeartbeat(
            SensorHeartbeatRequest request,
            TelemetrySourceType sourceType,
            String sourceTopic
    ) {
        String sensorId = request.getSensorId().trim();
        LocalDateTime heartbeatAt = request.getHeartbeatAt() == null ? LocalDateTime.now() : request.getHeartbeatAt();
        SensorHeartbeat saved = sensorHeartbeatRepository.save(SensorHeartbeat.builder()
                .sensorId(sensorId)
                .sourceType(sourceType)
                .heartbeatAt(heartbeatAt)
                .receivedAt(LocalDateTime.now())
                .firmwareVersion(request.getFirmwareVersion())
                .batteryPercent(request.getBatteryPercent())
                .build());

        return IngestHeartbeatResponse.builder()
                .sensorId(saved.getSensorId())
                .batteryPercent(saved.getBatteryPercent())
                .heartbeatAt(saved.getHeartbeatAt())
                .build();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public IngestBatchResponse ingestBatch(SensorBatchIngestRequest request) {
        int telemetrySuccess = 0;
        int telemetryFailure = 0;
        int heartbeatSuccess = 0;
        int heartbeatFailure = 0;

        for (SensorTelemetryRequest telemetryRequest : request.getTelemetries()) {
            if (telemetryRequest.getGatewayId() == null || telemetryRequest.getGatewayId().isBlank()) {
                telemetryRequest.setGatewayId(request.getGatewayId());
            }
            try {
                requiresNewTransactionTemplate.executeWithoutResult(
                        status -> ingestTelemetry(telemetryRequest, TelemetrySourceType.HTTP, null)
                );
                telemetrySuccess++;
            } catch (Exception ex) {
                telemetryFailure++;
                if (shouldSaveBatchDeadLetter(ex)) {
                    sensorDeadLetterService.saveDeadLetter(
                            TelemetrySourceType.HTTP,
                            null,
                            telemetryRequest.getSensorId(),
                            telemetryRequest.getRawPayload(),
                            "BATCH_TELEMETRY_FAILURE",
                            ex.getMessage() == null ? "Unknown telemetry ingest failure" : ex.getMessage()
                    );
                }
                log.warn("Batch telemetry ingest failed: sensorId={}, error={}", telemetryRequest.getSensorId(), ex.getMessage());
            }
        }

        for (SensorHeartbeatRequest heartbeatRequest : request.getHeartbeats()) {
            if (heartbeatRequest.getGatewayId() == null || heartbeatRequest.getGatewayId().isBlank()) {
                heartbeatRequest.setGatewayId(request.getGatewayId());
            }
            try {
                requiresNewTransactionTemplate.executeWithoutResult(
                        status -> ingestHeartbeat(heartbeatRequest, TelemetrySourceType.HTTP, null)
                );
                heartbeatSuccess++;
            } catch (Exception ex) {
                heartbeatFailure++;
                if (shouldSaveBatchDeadLetter(ex)) {
                    sensorDeadLetterService.saveDeadLetter(
                            TelemetrySourceType.HTTP,
                            null,
                            heartbeatRequest.getSensorId(),
                            null,
                            "BATCH_HEARTBEAT_FAILURE",
                            ex.getMessage() == null ? "Unknown heartbeat ingest failure" : ex.getMessage()
                    );
                }
                log.warn("Batch heartbeat ingest failed: sensorId={}, error={}", heartbeatRequest.getSensorId(), ex.getMessage());
            }
        }

        return IngestBatchResponse.builder()
                .telemetrySuccessCount(telemetrySuccess)
                .telemetryFailureCount(telemetryFailure)
                .heartbeatSuccessCount(heartbeatSuccess)
                .heartbeatFailureCount(heartbeatFailure)
                .build();
    }

    private SensorTelemetry saveTelemetryWithRaceHandling(
            String sensorId,
            SensorTelemetryRequest request,
            SensorTelemetry telemetry
    ) {
        try {
            return sensorTelemetryRepository.save(telemetry);
        } catch (DataIntegrityViolationException ex) {
            if (request.getSequenceNo() != null) {
                return sensorTelemetryRepository
                        .findBySensorIdAndSequenceNoAndMeasuredAt(sensorId, request.getSequenceNo(), request.getMeasuredAt())
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    private IngestTelemetryResponse buildDuplicateResponse(String sensorId, SensorTelemetryRequest request) {
        return sensorTelemetryRepository
                .findBySensorIdAndSequenceNoAndMeasuredAt(sensorId, request.getSequenceNo(), request.getMeasuredAt())
                .map(existing -> IngestTelemetryResponse.builder()
                        .telemetryId(existing.getId())
                        .sensorId(sensorId)
                        .qualityStatus(TelemetryQualityStatus.DUPLICATE)
                        .occupied(existing.isOccupied())
                        .duplicate(true)
                        .measuredAt(existing.getMeasuredAt())
                        .build())
                .orElseGet(() -> IngestTelemetryResponse.builder()
                        .telemetryId(null)
                        .sensorId(sensorId)
                        .qualityStatus(TelemetryQualityStatus.DUPLICATE)
                        .occupied(false)
                        .duplicate(true)
                        .measuredAt(request.getMeasuredAt())
                        .build());
    }

    private TelemetryQualityStatus determineQuality(String sensorId, SensorTelemetryRequest request) {
        if (request.getSequenceNo() != null &&
                sensorTelemetryRepository.existsBySensorIdAndSequenceNoAndMeasuredAt(
                        sensorId,
                        request.getSequenceNo(),
                        request.getMeasuredAt()
                )) {
            return TelemetryQualityStatus.DUPLICATE;
        }

        if (request.getDistanceCm() == null && request.getOccupied() == null) {
            throw new SensorException.ValidationException("Either distanceCm or occupied is required");
        }

        if (request.getDistanceCm() != null &&
                (request.getDistanceCm() < ingestionProperties.getMinDistanceCm() ||
                        request.getDistanceCm() > ingestionProperties.getMaxDistanceCm())) {
            return TelemetryQualityStatus.OUTLIER;
        }

        LocalDateTime staleCutoff = LocalDateTime.now().minusSeconds(ingestionProperties.getStaleThresholdSeconds());
        if (request.getMeasuredAt().isBefore(staleCutoff)) {
            return TelemetryQualityStatus.STALE;
        }

        return TelemetryQualityStatus.VALID;
    }

    private boolean shouldSaveBatchDeadLetter(Exception ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof SensorException.ResourceNotFoundException) {
                return false;
            }
            cursor = cursor.getCause();
        }
        return true;
    }
}
