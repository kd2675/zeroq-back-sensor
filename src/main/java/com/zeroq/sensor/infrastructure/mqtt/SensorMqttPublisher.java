package com.zeroq.sensor.infrastructure.mqtt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sensor.mqtt", name = "enabled", havingValue = "true")
public class SensorMqttPublisher {
    private final MessageChannel mqttOutboundChannel;

    public SensorMqttPublisher(@Qualifier("mqttOutboundChannel") MessageChannel mqttOutboundChannel) {
        this.mqttOutboundChannel = mqttOutboundChannel;
    }

    public void publish(String topic, String payload, int qos) {
        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .setHeader(MqttHeaders.QOS, qos)
                .build();
        mqttOutboundChannel.send(message);
    }
}
