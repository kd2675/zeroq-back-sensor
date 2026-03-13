package com.zeroq.sensor.service.ingest.biz;

import com.zeroq.sensor.database.pub.entity.GatewayHeartbeat;
import com.zeroq.sensor.database.pub.entity.GatewayStatus;
import com.zeroq.sensor.database.pub.entity.GatewayStatusSnapshot;
import com.zeroq.sensor.database.pub.repository.GatewayHeartbeatRepository;
import com.zeroq.sensor.database.pub.repository.GatewayStatusSnapshotRepository;
import com.zeroq.sensor.service.ingest.vo.GatewayStatusRequest;
import com.zeroq.sensor.service.ingest.vo.IngestGatewayStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatewayStatusIngestService {
    private final GatewayHeartbeatRepository gatewayHeartbeatRepository;
    private final GatewayStatusSnapshotRepository gatewayStatusSnapshotRepository;

    @Transactional
    public IngestGatewayStatusResponse ingestGatewayStatus(GatewayStatusRequest request) {
        GatewayStatus status = resolveStatus(request.getStatus());
        GatewayHeartbeat heartbeat = GatewayHeartbeat.builder()
                .gatewayId(request.getGatewayId())
                .status(status)
                .heartbeatAt(request.getHeartbeatAt())
                .firmwareVersion(blankToNull(request.getFirmwareVersion()))
                .ipAddress(blankToNull(request.getIpAddress()))
                .currentSensorLoad(defaultInt(request.getCurrentSensorLoad()))
                .latencyMs(request.getLatencyMs())
                .packetLossPercent(request.getPacketLossPercent())
                .telemetryPending(defaultLong(request.getTelemetryPending()))
                .telemetryFailed(defaultLong(request.getTelemetryFailed()))
                .heartbeatPending(defaultLong(request.getHeartbeatPending()))
                .heartbeatFailed(defaultLong(request.getHeartbeatFailed()))
                .commandDispatchPending(defaultLong(request.getCommandDispatchPending()))
                .commandAckPending(defaultLong(request.getCommandAckPending()))
                .rawPayload(blankToNull(request.getRawPayload()))
                .build();
        gatewayHeartbeatRepository.save(heartbeat);

        GatewayStatusSnapshot snapshot = gatewayStatusSnapshotRepository.findByGatewayId(request.getGatewayId())
                .orElseGet(() -> GatewayStatusSnapshot.builder()
                        .gatewayId(request.getGatewayId())
                        .build());
        snapshot.setStatus(status);
        snapshot.setLastHeartbeatAt(request.getHeartbeatAt());
        snapshot.setFirmwareVersion(blankToNull(request.getFirmwareVersion()));
        snapshot.setIpAddress(blankToNull(request.getIpAddress()));
        snapshot.setCurrentSensorLoad(defaultInt(request.getCurrentSensorLoad()));
        snapshot.setLatencyMs(request.getLatencyMs());
        snapshot.setPacketLossPercent(request.getPacketLossPercent());
        snapshot.setTelemetryPending(defaultLong(request.getTelemetryPending()));
        snapshot.setTelemetryFailed(defaultLong(request.getTelemetryFailed()));
        snapshot.setHeartbeatPending(defaultLong(request.getHeartbeatPending()));
        snapshot.setHeartbeatFailed(defaultLong(request.getHeartbeatFailed()));
        snapshot.setCommandDispatchPending(defaultLong(request.getCommandDispatchPending()));
        snapshot.setCommandAckPending(defaultLong(request.getCommandAckPending()));
        gatewayStatusSnapshotRepository.save(snapshot);

        return IngestGatewayStatusResponse.builder()
                .gatewayId(request.getGatewayId())
                .status(status.name())
                .heartbeatAt(request.getHeartbeatAt())
                .build();
    }

    private GatewayStatus resolveStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return GatewayStatus.UNKNOWN;
        }
        try {
            return GatewayStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return GatewayStatus.UNKNOWN;
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
