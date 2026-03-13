package com.zeroq.sensor.service.ingest.biz;

import com.zeroq.sensor.database.pub.entity.TelemetryQualityStatus;
import com.zeroq.sensor.database.pub.entity.TelemetrySourceType;
import com.zeroq.sensor.database.pub.repository.GatewayStatusSnapshotRepository;
import com.zeroq.sensor.database.pub.repository.SensorTelemetryRepository;
import com.zeroq.sensor.service.ingest.vo.GatewayStatusRequest;
import com.zeroq.sensor.service.ingest.vo.IngestGatewayStatusResponse;
import com.zeroq.sensor.service.ingest.vo.IngestTelemetryResponse;
import com.zeroq.sensor.service.ingest.vo.SensorBatchIngestRequest;
import com.zeroq.sensor.service.ingest.vo.IngestBatchResponse;
import com.zeroq.sensor.service.ingest.vo.SensorTelemetryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SensorIngestServiceTests {

    @Autowired
    private SensorIngestService sensorIngestService;

    @Autowired
    private SensorTelemetryRepository sensorTelemetryRepository;

    @Autowired
    private GatewayStatusIngestService gatewayStatusIngestService;

    @Autowired
    private GatewayStatusSnapshotRepository gatewayStatusSnapshotRepository;

    private String sensorId;

    @BeforeEach
    void setUp() {
        sensorId = "SN-T-" + System.nanoTime();
    }

    @Test
    void ingestTelemetryShouldPersistRawTelemetry() {
        SensorTelemetryRequest request = new SensorTelemetryRequest();
        request.setSensorId(sensorId);
        request.setSequenceNo(1L);
        request.setMeasuredAt(LocalDateTime.now());
        request.setDistanceCm(100.0);
        request.setGatewayId("GW-TEST-01");

        IngestTelemetryResponse response = sensorIngestService.ingestTelemetry(request, TelemetrySourceType.HTTP, null);

        assertThat(response.getTelemetryId()).isNotNull();
        assertThat(response.isOccupied()).isTrue();
        assertThat(response.getQualityStatus()).isEqualTo(TelemetryQualityStatus.VALID);
        assertThat(sensorTelemetryRepository.findTop200BySensorIdOrderByMeasuredAtDesc(sensorId))
                .hasSize(1);
    }

    @Test
    void duplicateSequenceShouldBeMarkedAsDuplicate() {
        LocalDateTime measuredAt = LocalDateTime.now();

        SensorTelemetryRequest first = new SensorTelemetryRequest();
        first.setSensorId(sensorId);
        first.setSequenceNo(777L);
        first.setMeasuredAt(measuredAt);
        first.setDistanceCm(150.0);

        sensorIngestService.ingestTelemetry(first, TelemetrySourceType.HTTP, null);

        SensorTelemetryRequest duplicate = new SensorTelemetryRequest();
        duplicate.setSensorId(sensorId);
        duplicate.setSequenceNo(777L);
        duplicate.setMeasuredAt(measuredAt);
        duplicate.setDistanceCm(150.0);

        IngestTelemetryResponse response = sensorIngestService.ingestTelemetry(duplicate, TelemetrySourceType.HTTP, null);

        assertThat(response.isDuplicate()).isTrue();
        assertThat(response.getQualityStatus()).isEqualTo(TelemetryQualityStatus.DUPLICATE);
    }

    @Test
    void ingestBatchShouldContinueOnFailure() {
        SensorTelemetryRequest successTelemetry = new SensorTelemetryRequest();
        successTelemetry.setSensorId(sensorId);
        successTelemetry.setSequenceNo(1001L);
        successTelemetry.setMeasuredAt(LocalDateTime.now());
        successTelemetry.setDistanceCm(110.0);

        SensorTelemetryRequest failedTelemetry = new SensorTelemetryRequest();
        failedTelemetry.setSensorId("INVALID-BATCH");
        failedTelemetry.setSequenceNo(2001L);
        failedTelemetry.setMeasuredAt(LocalDateTime.now());

        SensorBatchIngestRequest batchRequest = new SensorBatchIngestRequest();
        batchRequest.setGatewayId("GW-BATCH-01");
        batchRequest.setTelemetries(List.of(successTelemetry, failedTelemetry));

        IngestBatchResponse response = sensorIngestService.ingestBatch(batchRequest);

        assertThat(response.getTelemetrySuccessCount()).isEqualTo(1);
        assertThat(response.getTelemetryFailureCount()).isEqualTo(1);
        assertThat(sensorTelemetryRepository.findTop200BySensorIdOrderByMeasuredAtDesc(sensorId)).isNotEmpty();
    }

    @Test
    void telemetryShouldIngestWithoutPlaceContext() {
        SensorTelemetryRequest request = new SensorTelemetryRequest();
        request.setSensorId(sensorId);
        request.setSequenceNo(9101L);
        request.setMeasuredAt(LocalDateTime.now());
        request.setDistanceCm(90.0);

        IngestTelemetryResponse response = sensorIngestService.ingestTelemetry(request, TelemetrySourceType.HTTP, null);

        assertThat(response.getTelemetryId()).isNotNull();
        assertThat(response.getQualityStatus()).isEqualTo(TelemetryQualityStatus.VALID);
    }

    @Test
    void seatOccupancyTelemetryShouldIngestWithoutDistance() {
        SensorTelemetryRequest request = new SensorTelemetryRequest();
        request.setSensorId(sensorId);
        request.setSequenceNo(9201L);
        request.setMeasuredAt(LocalDateTime.now());
        request.setOccupied(true);
        request.setPadLeftValue(640);
        request.setPadRightValue(618);

        IngestTelemetryResponse response = sensorIngestService.ingestTelemetry(request, TelemetrySourceType.HTTP, null);

        assertThat(response.getTelemetryId()).isNotNull();
        assertThat(response.isOccupied()).isTrue();
        assertThat(sensorTelemetryRepository.findTop200BySensorIdOrderByMeasuredAtDesc(sensorId))
                .first()
                .satisfies(telemetry -> {
                    assertThat(telemetry.getDistanceCm()).isNull();
                    assertThat(telemetry.isOccupied()).isTrue();
                    assertThat(telemetry.getPadLeftValue()).isEqualTo(640);
                    assertThat(telemetry.getPadRightValue()).isEqualTo(618);
                });
    }

    @Test
    void ingestGatewayHeartbeatShouldPersistSnapshot() {
        GatewayStatusRequest request = new GatewayStatusRequest();
        request.setGatewayId("GW-RUNTIME-01");
        request.setStatus("ONLINE");
        request.setHeartbeatAt(LocalDateTime.now());
        request.setFirmwareVersion("v4.2.1");
        request.setIpAddress("10.20.30.40");
        request.setCurrentSensorLoad(7);
        request.setLatencyMs(18);
        request.setPacketLossPercent(1.2);
        request.setTelemetryPending(2L);
        request.setHeartbeatPending(1L);

        IngestGatewayStatusResponse response = gatewayStatusIngestService.ingestGatewayStatus(request);

        assertThat(response.getGatewayId()).isEqualTo("GW-RUNTIME-01");
        assertThat(response.getStatus()).isEqualTo("ONLINE");
        assertThat(gatewayStatusSnapshotRepository.findByGatewayId("GW-RUNTIME-01"))
                .get()
                .extracting("currentSensorLoad", "latencyMs", "telemetryPending")
                .containsExactly(7, 18, 2L);
    }
}
