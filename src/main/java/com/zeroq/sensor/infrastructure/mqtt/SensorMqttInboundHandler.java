package com.zeroq.sensor.infrastructure.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeroq.sensor.database.pub.entity.TelemetrySourceType;
import com.zeroq.sensor.service.command.biz.SensorCommandService;
import com.zeroq.sensor.service.command.vo.AckSensorCommandRequest;
import com.zeroq.sensor.service.command.vo.MqttCommandAckPayload;
import com.zeroq.sensor.service.ingest.biz.SensorDeadLetterService;
import com.zeroq.sensor.service.ingest.biz.SensorIngestService;
import com.zeroq.sensor.service.ingest.vo.SensorHeartbeatRequest;
import com.zeroq.sensor.service.ingest.vo.SensorTelemetryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sensor.mqtt", name = "enabled", havingValue = "true")
public class SensorMqttInboundHandler {
    private static final String MQTT_TOPIC_DELIMITER = "/";

    private final ObjectMapper objectMapper;
    private final SensorIngestService sensorIngestService;
    private final SensorDeadLetterService sensorDeadLetterService;
    private final SensorCommandService sensorCommandService;

    @ServiceActivator(inputChannel = "mqttTelemetryInputChannel")
    public void onTelemetryMessage(Message<?> message) {
        String topic = resolveTopic(message);
        String payload = resolvePayload(message.getPayload());

        try {
            SensorTelemetryRequest request = objectMapper.readValue(payload, SensorTelemetryRequest.class);
            if (request.getSensorId() == null || request.getSensorId().isBlank()) {
                request.setSensorId(extractSensorIdFromTopic(topic));
            }
            sensorIngestService.ingestTelemetry(request, TelemetrySourceType.MQTT, topic);
        } catch (Exception ex) {
            sensorDeadLetterService.saveDeadLetter(
                    TelemetrySourceType.MQTT,
                    topic,
                    extractSensorIdFromTopic(topic),
                    payload,
                    "MQTT_TELEMETRY_PARSE_OR_INGEST_ERROR",
                    ex.getMessage() == null ? "Unknown MQTT telemetry error" : ex.getMessage()
            );
            log.warn("MQTT telemetry processing failed: topic={}, error={}", topic, ex.getMessage());
        }
    }

    @ServiceActivator(inputChannel = "mqttHeartbeatInputChannel")
    public void onHeartbeatMessage(Message<?> message) {
        String topic = resolveTopic(message);
        String payload = resolvePayload(message.getPayload());

        try {
            SensorHeartbeatRequest request = objectMapper.readValue(payload, SensorHeartbeatRequest.class);
            if (request.getSensorId() == null || request.getSensorId().isBlank()) {
                request.setSensorId(extractSensorIdFromTopic(topic));
            }
            sensorIngestService.ingestHeartbeat(request, TelemetrySourceType.MQTT, topic);
        } catch (Exception ex) {
            sensorDeadLetterService.saveDeadLetter(
                    TelemetrySourceType.MQTT,
                    topic,
                    extractSensorIdFromTopic(topic),
                    payload,
                    "MQTT_HEARTBEAT_PARSE_OR_INGEST_ERROR",
                    ex.getMessage() == null ? "Unknown MQTT heartbeat error" : ex.getMessage()
            );
            log.warn("MQTT heartbeat processing failed: topic={}, error={}", topic, ex.getMessage());
        }
    }

    @ServiceActivator(inputChannel = "mqttCommandAckInputChannel")
    public void onCommandAckMessage(Message<?> message) {
        String topic = resolveTopic(message);
        String payload = resolvePayload(message.getPayload());

        try {
            MqttCommandAckPayload ackPayload = objectMapper.readValue(payload, MqttCommandAckPayload.class);
            if (ackPayload.getCommandId() == null) {
                throw new IllegalArgumentException("commandId is required in command ACK payload");
            }
            if (ackPayload.getStatus() == null) {
                throw new IllegalArgumentException("status is required in command ACK payload");
            }

            AckSensorCommandRequest request = new AckSensorCommandRequest();
            request.setStatus(ackPayload.getStatus());
            request.setAckPayload(ackPayload.getAckPayload());
            request.setFailureReason(ackPayload.getFailureReason());
            request.setAcknowledgedAt(ackPayload.getAcknowledgedAt());

            sensorCommandService.acknowledgeCommand(ackPayload.getCommandId(), request);
        } catch (Exception ex) {
            sensorDeadLetterService.saveDeadLetter(
                    TelemetrySourceType.MQTT,
                    topic,
                    extractSensorIdFromTopic(topic),
                    payload,
                    "MQTT_COMMAND_ACK_ERROR",
                    ex.getMessage() == null ? "Unknown MQTT command ACK error" : ex.getMessage()
            );
            log.warn("MQTT command ACK processing failed: topic={}, error={}", topic, ex.getMessage());
        }
    }

    private String resolveTopic(Message<?> message) {
        Object topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        return topic == null ? "" : topic.toString();
    }

    private String resolvePayload(Object payload) {
        if (payload == null) {
            return "";
        }
        if (payload instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return payload.toString();
    }

    private String extractSensorIdFromTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return "unknown-sensor";
        }
        List<String> tokens = List.of(topic.split(MQTT_TOPIC_DELIMITER));
        if (tokens.size() < 3) {
            return "unknown-sensor";
        }
        return tokens.get(2);
    }
}
