package com.zeroq.sensor.service.command.biz;

import com.zeroq.sensor.database.pub.entity.SensorCommand;
import com.zeroq.sensor.database.pub.entity.SensorCommandStatus;
import com.zeroq.sensor.database.pub.repository.SensorCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorCommandFailureService {
    private final SensorCommandRepository sensorCommandRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDispatchFailed(Long commandId, String reason) {
        SensorCommand command = sensorCommandRepository.findById(commandId).orElse(null);
        if (command == null) {
            return;
        }
        command.setStatus(SensorCommandStatus.FAILED);
        command.setFailureReason(reason);
        command.setAcknowledgedAt(LocalDateTime.now());
        sensorCommandRepository.save(command);
        log.warn("Command marked as FAILED after dispatch error: commandId={}, reason={}", commandId, reason);
    }
}
