package com.zeroq.sensor.service.ingest.biz;

import com.zeroq.sensor.common.config.SensorIngestionProperties;
import com.zeroq.sensor.common.exception.SensorException;
import com.zeroq.sensor.database.pub.entity.*;
import com.zeroq.sensor.database.pub.repository.SensorTelemetryRepository;
import com.zeroq.sensor.service.device.biz.SensorDeviceService;
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
    private final SensorDeviceService sensorDeviceService;
    private final SensorTelemetryRepository sensorTelemetryRepository;
    private final SensorDeadLetterService sensorDeadLetterService;
    private final SnapshotAggregationService snapshotAggregationService;
    private final SensorIngestionProperties ingestionProperties;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public SensorIngestService(
            SensorDeviceService sensorDeviceService,
            SensorTelemetryRepository sensorTelemetryRepository,
            SensorDeadLetterService sensorDeadLetterService,
            SnapshotAggregationService snapshotAggregationService,
            SensorIngestionProperties ingestionProperties,
            PlatformTransactionManager transactionManager
    ) {
        this.sensorDeviceService = sensorDeviceService;
        this.sensorTelemetryRepository = sensorTelemetryRepository;
        this.sensorDeadLetterService = sensorDeadLetterService;
        this.snapshotAggregationService = snapshotAggregationService;
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
        SensorDevice sensorDevice = resolveSensorDevice(request.getSensorId(), sourceType, sourceTopic, request.getRawPayload());
        Long resolvedPlaceId = resolvePlaceId(sensorDevice, request.getPlaceId());

        if (sensorDevice.getPlaceId() == null) {
            sensorDevice.setPlaceId(resolvedPlaceId);
        }

        TelemetryQualityStatus qualityStatus = determineQuality(sensorDevice, request);
        if (qualityStatus == TelemetryQualityStatus.DUPLICATE) {
            return buildDuplicateResponse(sensorDevice, request, resolvedPlaceId);
        }

        double adjustedDistance = request.getDistanceCm() + sensorDevice.getCalibrationOffsetCm();
        boolean occupied = adjustedDistance <= sensorDevice.getOccupancyThresholdCm();

        SensorTelemetry telemetry = SensorTelemetry.builder()
                .sensorDevice(sensorDevice)
                .sequenceNo(request.getSequenceNo())
                .placeId(resolvedPlaceId)
                .gatewayId(resolveGatewayId(request.getGatewayId()))
                .sourceType(sourceType)
                .measuredAt(request.getMeasuredAt())
                .receivedAt(LocalDateTime.now())
                .distanceCm(request.getDistanceCm())
                .occupied(occupied)
                .confidence(request.getConfidence())
                .temperatureC(request.getTemperatureC())
                .humidityPercent(request.getHumidityPercent())
                .batteryPercent(request.getBatteryPercent())
                .rssi(request.getRssi())
                .qualityStatus(qualityStatus)
                .rawPayload(request.getRawPayload())
                .build();

        SensorTelemetry saved = saveTelemetryWithRaceHandling(sensorDevice, request, telemetry);

        sensorDevice.markTelemetry(request.getSequenceNo(), request.getBatteryPercent(), request.getMeasuredAt());
        sensorDeviceService.saveEntity(sensorDevice);

        if (qualityStatus == TelemetryQualityStatus.VALID) {
            snapshotAggregationService.recalculateIfPossible(saved.getPlaceId());
        }

        return IngestTelemetryResponse.builder()
                .telemetryId(saved.getId())
                .sensorId(sensorDevice.getSensorId())
                .placeId(saved.getPlaceId())
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
        SensorDevice sensorDevice = resolveSensorDevice(request.getSensorId(), sourceType, sourceTopic, null);
        validateAndApplyHeartbeatPlaceId(sensorDevice, request.getPlaceId());

        LocalDateTime heartbeatAt = request.getHeartbeatAt() == null ? LocalDateTime.now() : request.getHeartbeatAt();
        sensorDevice.markHeartbeat(heartbeatAt, request.getBatteryPercent(), request.getFirmwareVersion());
        SensorDevice saved = sensorDeviceService.saveEntity(sensorDevice);

        return IngestHeartbeatResponse.builder()
                .sensorId(saved.getSensorId())
                .placeId(saved.getPlaceId())
                .batteryPercent(saved.getBatteryPercent())
                .heartbeatAt(saved.getLastHeartbeatAt())
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
            SensorDevice sensorDevice,
            SensorTelemetryRequest request,
            SensorTelemetry telemetry
    ) {
        try {
            return sensorTelemetryRepository.save(telemetry);
        } catch (DataIntegrityViolationException ex) {
            if (request.getSequenceNo() != null) {
                return sensorTelemetryRepository
                        .findBySensorDeviceAndSequenceNoAndMeasuredAt(sensorDevice, request.getSequenceNo(), request.getMeasuredAt())
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    private IngestTelemetryResponse buildDuplicateResponse(
            SensorDevice sensorDevice,
            SensorTelemetryRequest request,
            Long placeId
    ) {
        return sensorTelemetryRepository
                .findBySensorDeviceAndSequenceNoAndMeasuredAt(sensorDevice, request.getSequenceNo(), request.getMeasuredAt())
                .map(existing -> IngestTelemetryResponse.builder()
                        .telemetryId(existing.getId())
                        .sensorId(sensorDevice.getSensorId())
                        .placeId(existing.getPlaceId())
                        .qualityStatus(TelemetryQualityStatus.DUPLICATE)
                        .occupied(existing.isOccupied())
                        .duplicate(true)
                        .measuredAt(existing.getMeasuredAt())
                        .build())
                .orElseGet(() -> IngestTelemetryResponse.builder()
                        .telemetryId(null)
                        .sensorId(sensorDevice.getSensorId())
                        .placeId(placeId)
                        .qualityStatus(TelemetryQualityStatus.DUPLICATE)
                        .occupied(false)
                        .duplicate(true)
                        .measuredAt(request.getMeasuredAt())
                        .build());
    }

    private TelemetryQualityStatus determineQuality(SensorDevice sensorDevice, SensorTelemetryRequest request) {
        if (request.getSequenceNo() != null &&
                sensorTelemetryRepository.existsBySensorDeviceAndSequenceNoAndMeasuredAt(
                        sensorDevice,
                        request.getSequenceNo(),
                        request.getMeasuredAt()
                )) {
            return TelemetryQualityStatus.DUPLICATE;
        }

        if (request.getDistanceCm() < ingestionProperties.getMinDistanceCm() ||
                request.getDistanceCm() > ingestionProperties.getMaxDistanceCm()) {
            return TelemetryQualityStatus.OUTLIER;
        }

        LocalDateTime staleCutoff = LocalDateTime.now().minusSeconds(ingestionProperties.getStaleThresholdSeconds());
        if (request.getMeasuredAt().isBefore(staleCutoff)) {
            return TelemetryQualityStatus.STALE;
        }

        return TelemetryQualityStatus.VALID;
    }

    private SensorDevice resolveSensorDevice(
            String sensorId,
            TelemetrySourceType sourceType,
            String sourceTopic,
            String payload
    ) {
        try {
            return sensorDeviceService.findEntityBySensorId(sensorId);
        } catch (SensorException.ResourceNotFoundException ex) {
            if (!ingestionProperties.isAutoRegisterUnknownSensor()) {
                sensorDeadLetterService.saveDeadLetter(
                        sourceType,
                        sourceTopic,
                        sensorId,
                        payload,
                        "UNKNOWN_SENSOR",
                        ex.getMessage()
                );
                throw ex;
            }

            SensorDevice autoRegistered = SensorDevice.builder()
                    .sensorId(sensorId)
                    .macAddress(buildAutoMacAddress(sensorId))
                    .model("AUTO_REGISTERED")
                    .type(SensorType.OCCUPANCY_DETECTION)
                    .protocol(sourceType == TelemetrySourceType.MQTT ? SensorProtocol.MQTT : SensorProtocol.HTTP)
                    .status(SensorStatus.ACTIVE)
                    .occupancyThresholdCm(ingestionProperties.getDefaultOccupancyThresholdCm())
                    .build();
            return sensorDeviceService.saveEntity(autoRegistered);
        }
    }

    private Long resolvePlaceId(SensorDevice sensorDevice, Long placeIdFromRequest) {
        Long installedPlaceId = sensorDevice.getPlaceId();

        if (installedPlaceId != null) {
            if (placeIdFromRequest != null && !installedPlaceId.equals(placeIdFromRequest)) {
                throw new SensorException.ValidationException(
                        "Telemetry placeId mismatch. installedPlaceId=" + installedPlaceId + ", requestPlaceId=" + placeIdFromRequest
                );
            }
            return installedPlaceId;
        }

        if (placeIdFromRequest == null) {
            throw new SensorException.ValidationException("placeId is required for sensor telemetry when sensor is not installed");
        }
        return placeIdFromRequest;
    }

    private void validateAndApplyHeartbeatPlaceId(SensorDevice sensorDevice, Long placeIdFromRequest) {
        if (placeIdFromRequest == null) {
            return;
        }

        if (sensorDevice.getPlaceId() == null) {
            sensorDevice.setPlaceId(placeIdFromRequest);
            return;
        }

        if (!sensorDevice.getPlaceId().equals(placeIdFromRequest)) {
            throw new SensorException.ValidationException(
                    "Heartbeat placeId mismatch. installedPlaceId=" + sensorDevice.getPlaceId() + ", requestPlaceId=" + placeIdFromRequest
            );
        }
    }

    private String resolveGatewayId(String gatewayId) {
        if (gatewayId == null || gatewayId.isBlank()) {
            return "unknown-gateway";
        }
        return gatewayId;
    }

    private String buildAutoMacAddress(String sensorId) {
        long positiveHash = Integer.toUnsignedLong(sensorId.hashCode());
        return String.format("AUTO-%014d", positiveHash);
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
