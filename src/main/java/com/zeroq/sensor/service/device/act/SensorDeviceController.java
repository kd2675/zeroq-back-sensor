package com.zeroq.sensor.service.device.act;

import com.zeroq.sensor.common.security.SensorRoleGuard;
import com.zeroq.sensor.service.device.biz.SensorDeviceService;
import com.zeroq.sensor.service.device.vo.InstallSensorRequest;
import com.zeroq.sensor.service.device.vo.RegisterSensorRequest;
import com.zeroq.sensor.service.device.vo.SensorDeviceResponse;
import com.zeroq.sensor.service.device.vo.UpdateSensorStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import web.common.core.response.base.dto.ResponseDataDTO;

import java.util.List;

@RestController
@RequestMapping("/api/zeroq/v1/sensor/devices")
@RequiredArgsConstructor
public class SensorDeviceController {
    private final SensorDeviceService sensorDeviceService;
    private final SensorRoleGuard sensorRoleGuard;

    @PostMapping
    public ResponseDataDTO<SensorDeviceResponse> registerSensor(
            @Valid @RequestBody RegisterSensorRequest request,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(sensorDeviceService.registerSensor(request), "센서 등록 완료");
    }

    @PutMapping("/{sensorId}/install")
    public ResponseDataDTO<SensorDeviceResponse> installSensor(
            @PathVariable String sensorId,
            @Valid @RequestBody InstallSensorRequest request,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(sensorDeviceService.installSensor(sensorId, request), "센서 설치 정보 반영 완료");
    }

    @PatchMapping("/{sensorId}/status")
    public ResponseDataDTO<SensorDeviceResponse> updateSensorStatus(
            @PathVariable String sensorId,
            @Valid @RequestBody UpdateSensorStatusRequest request,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(sensorDeviceService.updateSensorStatus(sensorId, request.getStatus()), "센서 상태 변경 완료");
    }

    @GetMapping("/{sensorId}")
    public ResponseDataDTO<SensorDeviceResponse> getSensor(
            @PathVariable String sensorId,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(sensorDeviceService.getSensor(sensorId), "센서 조회 완료");
    }

    @GetMapping
    public ResponseDataDTO<List<SensorDeviceResponse>> getSensors(
            @RequestParam(required = false) Long placeId,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(sensorDeviceService.getSensors(placeId), "센서 목록 조회 완료");
    }
}
