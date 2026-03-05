package com.zeroq.sensor.service.ingest.biz;

import com.zeroq.sensor.database.pub.entity.SensorType;
import com.zeroq.sensor.database.pub.entity.TelemetryQualityStatus;
import com.zeroq.sensor.database.pub.entity.TelemetrySourceType;
import com.zeroq.sensor.database.pub.repository.PlaceOccupancySnapshotRepository;
import com.zeroq.sensor.database.pub.repository.SensorTelemetryRepository;
import com.zeroq.sensor.service.device.biz.SensorDeviceService;
import com.zeroq.sensor.service.device.vo.InstallSensorRequest;
import com.zeroq.sensor.service.device.vo.RegisterSensorRequest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SensorIngestServiceTests {

    @Autowired
    private SensorIngestService sensorIngestService;

    @Autowired
    private SensorDeviceService sensorDeviceService;

    @Autowired
    private PlaceOccupancySnapshotRepository placeOccupancySnapshotRepository;

    @Autowired
    private SensorTelemetryRepository sensorTelemetryRepository;

    private String sensorId;
    private Long placeId;

    @BeforeEach
    void setUp() {
        sensorId = "SN-T-" + System.nanoTime();
        placeId = 100000L + Math.floorMod(System.nanoTime(), 100000L);

        RegisterSensorRequest request = new RegisterSensorRequest();
        request.setSensorId(sensorId);
        request.setMacAddress("AA:BB:CC:%02d:%02d:%02d".formatted(
                (int) Math.floorMod(System.nanoTime(), 99),
                (int) Math.floorMod(System.nanoTime() / 100, 99),
                (int) Math.floorMod(System.nanoTime() / 10000, 99)
        ));
        request.setModel("ESP32-WROOM-32D");
        request.setType(SensorType.OCCUPANCY_DETECTION);
        request.setPlaceId(placeId);
        request.setPositionCode("TABLE-01");
        request.setOccupancyThresholdCm(120.0);
        sensorDeviceService.registerSensor(request);

        InstallSensorRequest install = new InstallSensorRequest();
        install.setPlaceId(placeId);
        install.setPositionCode("TABLE-01");
        install.setOccupancyThresholdCm(120.0);
        sensorDeviceService.installSensor(sensorId, install);
    }

    @Test
    void ingestTelemetryShouldCreateSnapshot() {
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

        var snapshot = placeOccupancySnapshotRepository.findByPlaceId(placeId).orElseThrow();
        assertThat(snapshot.getOccupiedCount()).isEqualTo(1);
        assertThat(snapshot.getActiveSensorCount()).isEqualTo(1);
        assertThat(snapshot.getOccupancyRate()).isEqualTo(100.0);
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
        failedTelemetry.setSensorId("UNKNOWN-SENSOR");
        failedTelemetry.setSequenceNo(2001L);
        failedTelemetry.setMeasuredAt(LocalDateTime.now());
        failedTelemetry.setDistanceCm(100.0);

        SensorBatchIngestRequest batchRequest = new SensorBatchIngestRequest();
        batchRequest.setGatewayId("GW-BATCH-01");
        batchRequest.setTelemetries(List.of(successTelemetry, failedTelemetry));

        IngestBatchResponse response = sensorIngestService.ingestBatch(batchRequest);

        assertThat(response.getTelemetrySuccessCount()).isEqualTo(1);
        assertThat(response.getTelemetryFailureCount()).isEqualTo(1);
        assertThat(sensorTelemetryRepository.findTop200ByPlaceIdOrderByMeasuredAtDesc(placeId)).isNotEmpty();
    }

    @Test
    void placeMismatchShouldFail() {
        SensorTelemetryRequest request = new SensorTelemetryRequest();
        request.setSensorId(sensorId);
        request.setSequenceNo(9101L);
        request.setMeasuredAt(LocalDateTime.now());
        request.setDistanceCm(90.0);
        request.setPlaceId(999L);

        assertThatThrownBy(() -> sensorIngestService.ingestTelemetry(request, TelemetrySourceType.HTTP, null))
                .hasMessageContaining("placeId mismatch");
    }
}
