package com.zeroq.sensor.service.ingest.act;

import com.zeroq.sensor.database.pub.entity.TelemetrySourceType;
import com.zeroq.sensor.common.security.SensorRoleGuard;
import com.zeroq.sensor.service.ingest.biz.GatewayStatusIngestService;
import com.zeroq.sensor.service.ingest.biz.SensorIngestService;
import com.zeroq.sensor.service.ingest.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import web.common.core.response.base.dto.ResponseDataDTO;

@RestController
@RequestMapping("/api/zeroq/v1/sensor/ingest")
@RequiredArgsConstructor
public class SensorIngestController {
    private final SensorIngestService sensorIngestService;
    private final GatewayStatusIngestService gatewayStatusIngestService;
    private final SensorRoleGuard sensorRoleGuard;

    @PostMapping("/telemetry")
    public ResponseDataDTO<IngestTelemetryResponse> ingestTelemetry(
            @Valid @RequestBody SensorTelemetryRequest request,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireGatewayOrManagerOrAdmin(httpServletRequest);
        IngestTelemetryResponse response = sensorIngestService.ingestTelemetry(request, TelemetrySourceType.HTTP, null);
        return ResponseDataDTO.of(response, "텔레메트리 수집 완료");
    }

    @PostMapping("/heartbeat")
    public ResponseDataDTO<IngestHeartbeatResponse> ingestHeartbeat(
            @Valid @RequestBody SensorHeartbeatRequest request,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireGatewayOrManagerOrAdmin(httpServletRequest);
        IngestHeartbeatResponse response = sensorIngestService.ingestHeartbeat(request, TelemetrySourceType.HTTP, null);
        return ResponseDataDTO.of(response, "하트비트 수집 완료");
    }

    @PostMapping("/batch")
    public ResponseDataDTO<IngestBatchResponse> ingestBatch(
            @Valid @RequestBody SensorBatchIngestRequest request,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireGatewayOrManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(sensorIngestService.ingestBatch(request), "배치 수집 처리 완료");
    }

    @PostMapping("/gateway-heartbeat")
    public ResponseDataDTO<IngestGatewayStatusResponse> ingestGatewayHeartbeat(
            @Valid @RequestBody GatewayStatusRequest request,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireGatewayOrManagerOrAdmin(httpServletRequest);
        IngestGatewayStatusResponse response = gatewayStatusIngestService.ingestGatewayStatus(request);
        return ResponseDataDTO.of(response, "게이트웨이 상태 수집 완료");
    }
}
