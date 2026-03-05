package com.zeroq.sensor.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sensor.ingestion")
public class SensorIngestionProperties {
    private long staleThresholdSeconds = 180;
    private long lookbackSeconds = 300;
    private double minDistanceCm = 2.0;
    private double maxDistanceCm = 400.0;
    private double defaultOccupancyThresholdCm = 120.0;
    private boolean autoRegisterUnknownSensor = false;
}
