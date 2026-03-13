package com.zeroq.sensor.service.command.act;

import com.zeroq.sensor.common.security.SensorRoleGuard;
import com.zeroq.sensor.service.command.biz.SensorCommandService;
import com.zeroq.sensor.service.command.vo.AckSensorCommandRequest;
import com.zeroq.sensor.service.command.vo.CreateSensorCommandRequest;
import com.zeroq.sensor.service.command.vo.SensorCommandResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import web.common.core.response.base.dto.ResponseDataDTO;

import java.util.List;

@RestController
@RequestMapping("/api/zeroq/v1/sensor/commands")
@RequiredArgsConstructor
public class SensorCommandController {
    private final SensorCommandService sensorCommandService;
    private final SensorRoleGuard sensorRoleGuard;

    @PostMapping
    public ResponseDataDTO<SensorCommandResponse> createCommand(
            @Valid @RequestBody CreateSensorCommandRequest request,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(sensorCommandService.createCommand(request), "센서 명령 생성 완료");
    }

    @GetMapping("/sensor/{sensorId}/pending")
    public ResponseDataDTO<List<SensorCommandResponse>> getPendingCommands(
            @PathVariable String sensorId,
            @RequestParam(defaultValue = "false") boolean markAsSent,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireGatewayOrManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(sensorCommandService.getPendingCommands(sensorId, markAsSent), "센서 명령 조회 완료");
    }

    @PatchMapping("/{commandId}/ack")
    public ResponseDataDTO<SensorCommandResponse> ackCommand(
            @PathVariable Long commandId,
            @Valid @RequestBody AckSensorCommandRequest request,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireGatewayOrManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(sensorCommandService.acknowledgeCommand(commandId, request), "센서 명령 ACK 처리 완료");
    }

    @PostMapping("/{commandId}/dispatch")
    public ResponseDataDTO<SensorCommandResponse> dispatchCommand(
            @PathVariable Long commandId,
            HttpServletRequest httpServletRequest
    ) {
        sensorRoleGuard.requireManagerOrAdmin(httpServletRequest);
        return ResponseDataDTO.of(sensorCommandService.dispatchCommand(commandId), "센서 명령 전송 완료");
    }
}
