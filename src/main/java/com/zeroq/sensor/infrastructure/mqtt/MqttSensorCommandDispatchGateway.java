package com.zeroq.sensor.infrastructure.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeroq.sensor.common.config.SensorMqttProperties;
import com.zeroq.sensor.common.exception.SensorException;
import com.zeroq.sensor.service.command.biz.SensorCommandDispatchGateway;
import com.zeroq.sensor.service.command.vo.SensorCommandDispatchMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sensor.mqtt", name = "enabled", havingValue = "true")
public class MqttSensorCommandDispatchGateway implements SensorCommandDispatchGateway {
    private final SensorMqttPublisher sensorMqttPublisher;
    private final SensorMqttProperties sensorMqttProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void dispatch(SensorCommandDispatchMessage message) {
        String topic = sensorMqttProperties.getCommandTopicPattern()
                .replace("{sensorId}", message.getSensorId());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commandId", message.getCommandId());
        payload.put("sensorId", message.getSensorId());
        payload.put("commandType", message.getCommandType().name());
        payload.put("requestedAt", message.getRequestedAt());
        payload.put("commandPayload", message.getCommandPayload());

        try {
            sensorMqttPublisher.publish(topic, objectMapper.writeValueAsString(payload), sensorMqttProperties.getQos());
        } catch (JsonProcessingException ex) {
            throw new SensorException.ValidationException("Failed to serialize MQTT command payload");
        }
    }
}
