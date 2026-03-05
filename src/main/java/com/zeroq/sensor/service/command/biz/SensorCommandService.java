package com.zeroq.sensor.service.command.biz;

import com.zeroq.sensor.common.exception.SensorException;
import com.zeroq.sensor.database.pub.entity.SensorCommand;
import com.zeroq.sensor.database.pub.entity.SensorCommandStatus;
import com.zeroq.sensor.database.pub.repository.SensorCommandRepository;
import com.zeroq.sensor.service.command.vo.AckSensorCommandRequest;
import com.zeroq.sensor.service.command.vo.CreateSensorCommandRequest;
import com.zeroq.sensor.service.command.vo.SensorCommandDispatchMessage;
import com.zeroq.sensor.service.command.vo.SensorCommandResponse;
import com.zeroq.sensor.service.device.biz.SensorDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorCommandService {
    private final SensorCommandRepository sensorCommandRepository;
    private final SensorDeviceService sensorDeviceService;
    private final ObjectProvider<SensorCommandDispatchGateway> dispatchGatewayProvider;
    private final SensorCommandFailureService sensorCommandFailureService;

    @Transactional
    public SensorCommandResponse createCommand(CreateSensorCommandRequest request) {
        SensorCommand command = SensorCommand.builder()
                .sensorDevice(sensorDeviceService.findEntityBySensorId(request.getSensorId()))
                .commandType(request.getCommandType())
                .commandPayload(request.getCommandPayload())
                .requestedBy(request.getRequestedBy())
                .requestedAt(LocalDateTime.now())
                .status(SensorCommandStatus.PENDING)
                .build();

        SensorCommand saved = sensorCommandRepository.save(command);
        log.info("Sensor command created: commandId={}, sensorId={}, type={}",
                saved.getId(),
                saved.getSensorDevice().getSensorId(),
                saved.getCommandType());
        return SensorCommandResponse.from(saved);
    }

    @Transactional
    public SensorCommandResponse markSent(Long commandId) {
        SensorCommand command = findById(commandId);
        command.markSent(LocalDateTime.now());
        SensorCommand saved = sensorCommandRepository.save(command);
        return SensorCommandResponse.from(saved);
    }

    @Transactional
    public SensorCommandResponse dispatchCommand(Long commandId) {
        SensorCommand command = findById(commandId);
        if (command.getStatus() != SensorCommandStatus.PENDING) {
            throw new SensorException.ConflictException("Command can only be dispatched from PENDING status");
        }

        SensorCommandDispatchGateway dispatchGateway = dispatchGatewayProvider.getIfAvailable();
        if (dispatchGateway == null) {
            throw new SensorException.ValidationException("MQTT dispatch gateway is disabled");
        }

        command.markSent(LocalDateTime.now());
        SensorCommand saved = sensorCommandRepository.save(command);
        SensorCommandDispatchMessage dispatchMessage = SensorCommandDispatchMessage.from(saved);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchAfterCommit(dispatchGateway, dispatchMessage, saved.getId());
                }
            });
        } else {
            dispatchAfterCommit(dispatchGateway, dispatchMessage, saved.getId());
        }

        log.info("Sensor command dispatched: commandId={}, sensorId={}",
                saved.getId(),
                saved.getSensorDevice().getSensorId());
        return SensorCommandResponse.from(saved);
    }

    @Transactional
    public SensorCommandResponse acknowledgeCommand(Long commandId, AckSensorCommandRequest request) {
        SensorCommand command = findById(commandId);
        LocalDateTime acknowledgedAt = request.getAcknowledgedAt() == null
                ? LocalDateTime.now()
                : request.getAcknowledgedAt();

        if (command.getStatus() == SensorCommandStatus.CANCELED) {
            throw new SensorException.ConflictException("Canceled command cannot be acknowledged");
        }
        if (command.getStatus() == SensorCommandStatus.ACKNOWLEDGED) {
            throw new SensorException.ConflictException("Command is already acknowledged");
        }

        if (request.getStatus() == SensorCommandStatus.ACKNOWLEDGED) {
            ensureAckTransitionAllowed(command.getStatus(), request.getStatus());
            command.markAcknowledged(acknowledgedAt, request.getAckPayload());
        } else if (request.getStatus() == SensorCommandStatus.FAILED) {
            ensureAckTransitionAllowed(command.getStatus(), request.getStatus());
            if (request.getFailureReason() == null || request.getFailureReason().isBlank()) {
                throw new SensorException.ValidationException("failureReason is required when status is FAILED");
            }
            command.markFailed(acknowledgedAt, request.getFailureReason(), request.getAckPayload());
        } else if (request.getStatus() == SensorCommandStatus.CANCELED) {
            ensureAckTransitionAllowed(command.getStatus(), request.getStatus());
            command.setStatus(SensorCommandStatus.CANCELED);
            command.setAcknowledgedAt(acknowledgedAt);
            command.setFailureReason(request.getFailureReason());
            command.setAckPayload(request.getAckPayload());
        } else {
            throw new SensorException.ValidationException("Unsupported ACK status: " + request.getStatus());
        }

        SensorCommand saved = sensorCommandRepository.save(command);
        log.info("Sensor command acked: commandId={}, status={}", saved.getId(), saved.getStatus());
        return SensorCommandResponse.from(saved);
    }

    @Transactional
    public List<SensorCommandResponse> getPendingCommands(String sensorId, boolean markAsSent) {
        List<SensorCommand> commands = sensorCommandRepository
                .findAllBySensorDeviceSensorIdAndStatusInOrderByRequestedAtAsc(
                        sensorId,
                        EnumSet.of(SensorCommandStatus.PENDING, SensorCommandStatus.SENT)
                );

        if (markAsSent) {
            LocalDateTime sentAt = LocalDateTime.now();
            commands.forEach(command -> {
                if (command.getStatus() == SensorCommandStatus.PENDING) {
                    command.markSent(sentAt);
                }
            });
            commands = sensorCommandRepository.saveAll(commands);
        }

        return commands.stream()
                .map(SensorCommandResponse::from)
                .toList();
    }

    private SensorCommand findById(Long commandId) {
        return sensorCommandRepository.findById(commandId)
                .orElseThrow(() -> new SensorException.ResourceNotFoundException("SensorCommand", "id", commandId));
    }

    private void dispatchAfterCommit(
            SensorCommandDispatchGateway dispatchGateway,
            SensorCommandDispatchMessage dispatchMessage,
            Long commandId
    ) {
        try {
            dispatchGateway.dispatch(dispatchMessage);
        } catch (Exception ex) {
            String reason = ex.getMessage() == null ? "Unknown dispatch error" : ex.getMessage();
            sensorCommandFailureService.markDispatchFailed(commandId, reason);
        }
    }

    private void ensureAckTransitionAllowed(SensorCommandStatus currentStatus, SensorCommandStatus targetStatus) {
        if (currentStatus != SensorCommandStatus.SENT && currentStatus != SensorCommandStatus.FAILED) {
            throw new SensorException.ConflictException(
                    "Invalid status transition. currentStatus=" + currentStatus + ", targetStatus=" + targetStatus
            );
        }
    }
}
