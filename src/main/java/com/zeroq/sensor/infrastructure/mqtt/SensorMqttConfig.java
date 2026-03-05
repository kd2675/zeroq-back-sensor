package com.zeroq.sensor.infrastructure.mqtt;

import com.zeroq.sensor.common.config.SensorMqttProperties;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sensor.mqtt", name = "enabled", havingValue = "true")
public class SensorMqttConfig {
    private final SensorMqttProperties sensorMqttProperties;

    @Bean
    public MqttPahoClientFactory mqttPahoClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{sensorMqttProperties.getBrokerUri()});
        if (sensorMqttProperties.getUsername() != null && !sensorMqttProperties.getUsername().isBlank()) {
            options.setUserName(sensorMqttProperties.getUsername());
        }
        if (sensorMqttProperties.getPassword() != null && !sensorMqttProperties.getPassword().isBlank()) {
            options.setPassword(sensorMqttProperties.getPassword().toCharArray());
        }
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        options.setConnectionTimeout(15);
        options.setKeepAliveInterval(20);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttTelemetryInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttHeartbeatInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttCommandAckInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer telemetryInboundAdapter(MqttPahoClientFactory mqttPahoClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                sensorMqttProperties.getClientId() + "-telemetry-in",
                mqttPahoClientFactory,
                sensorMqttProperties.getTelemetryTopic()
        );
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(sensorMqttProperties.getQos());
        adapter.setOutputChannel(mqttTelemetryInputChannel());
        return adapter;
    }

    @Bean
    public MessageProducer heartbeatInboundAdapter(MqttPahoClientFactory mqttPahoClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                sensorMqttProperties.getClientId() + "-heartbeat-in",
                mqttPahoClientFactory,
                sensorMqttProperties.getHeartbeatTopic()
        );
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(sensorMqttProperties.getQos());
        adapter.setOutputChannel(mqttHeartbeatInputChannel());
        return adapter;
    }

    @Bean
    public MessageProducer commandAckInboundAdapter(MqttPahoClientFactory mqttPahoClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                sensorMqttProperties.getClientId() + "-command-ack-in",
                mqttPahoClientFactory,
                sensorMqttProperties.getCommandAckTopic()
        );
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(sensorMqttProperties.getQos());
        adapter.setOutputChannel(mqttCommandAckInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound(MqttPahoClientFactory mqttPahoClientFactory) {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(
                sensorMqttProperties.getClientId() + "-outbound",
                mqttPahoClientFactory
        );
        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(sensorMqttProperties.getQos());
        messageHandler.setConverter(new DefaultPahoMessageConverter());
        return messageHandler;
    }
}
