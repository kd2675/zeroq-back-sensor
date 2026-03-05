package com.zeroq.sensor.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sensor.mqtt")
public class SensorMqttProperties {
    private boolean enabled = false;
    private String brokerUri;
    private String clientId;
    private String username;
    private String password;
    private int qos = 1;
    private String telemetryTopic;
    private String heartbeatTopic;
    private String commandAckTopic;
    private String commandTopicPattern;
}
