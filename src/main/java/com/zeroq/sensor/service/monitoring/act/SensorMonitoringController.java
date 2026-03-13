package com.zeroq.sensor.service.monitoring.act;

import com.zeroq.sensor.common.security.SensorRoleGuard;
import com.zeroq.sensor.service.monitoring.biz.SensorMonitoringService;
import com.zeroq.sensor.service.monitoring.vo.DeadLetterResponse;
import com.zeroq.sensor.service.monitoring.vo.RecentTelemetryResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import web.common.core.response.base.dto.ResponseDataDTO;

import java.util.List;

@RestController
@RequestMapping("/api/zeroq/v1/sensor/monitoring")
@RequiredArgsConstructor
public class SensorMonitoringController {
    private final SensorMonitoringService sensorMonitoringService;
    private final SensorRoleGuard sensorRoleGuard;

    @GetMapping("/sensors/{sensorId}/telemetry/recent")
    public ResponseDataDTO<List<RecentTelemetryResponse>> getRecentTelemetryBySensor(
            @PathVariable String sensorId,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(
                sensorMonitoringService.getRecentTelemetryBySensor(sensorId, limit),
                "센서별 최근 텔레메트리 조회 완료"
        );
    }

    @GetMapping("/dead-letters")
    public ResponseDataDTO<List<DeadLetterResponse>> getDeadLetters(
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(sensorMonitoringService.getRecentDeadLetters(limit), "데드레터 조회 완료");
    }
}
