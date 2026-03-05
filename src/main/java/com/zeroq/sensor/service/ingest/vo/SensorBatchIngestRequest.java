package com.zeroq.sensor.service.ingest.vo;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SensorBatchIngestRequest {
    private String gatewayId;

    @Valid
    private List<SensorTelemetryRequest> telemetries = new ArrayList<>();

    @Valid
    private List<SensorHeartbeatRequest> heartbeats = new ArrayList<>();
}
