package com.zeroq.sensor.service.command.biz;

import com.zeroq.sensor.database.pub.entity.SensorCommandStatus;
import com.zeroq.sensor.database.pub.entity.SensorCommandType;
import com.zeroq.sensor.database.pub.entity.SensorType;
import com.zeroq.sensor.service.command.vo.AckSensorCommandRequest;
import com.zeroq.sensor.service.command.vo.CreateSensorCommandRequest;
import com.zeroq.sensor.service.device.biz.SensorDeviceService;
import com.zeroq.sensor.service.device.vo.RegisterSensorRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SensorCommandServiceTests {

    @Autowired
    private SensorCommandService sensorCommandService;

    @Autowired
    private SensorDeviceService sensorDeviceService;

    private String sensorId;

    @BeforeEach
    void setUp() {
        sensorId = "SN-C-" + System.nanoTime();

        RegisterSensorRequest request = new RegisterSensorRequest();
        request.setSensorId(sensorId);
        request.setMacAddress("AA:BB:CC:%02d:%02d:%02d".formatted(
                (int) Math.floorMod(System.nanoTime(), 99),
                (int) Math.floorMod(System.nanoTime() / 100, 99),
                (int) Math.floorMod(System.nanoTime() / 10000, 99)
        ));
        request.setModel("ESP32-WROOM-32D");
        request.setType(SensorType.OCCUPANCY_DETECTION);
        request.setPlaceId(201L);
        sensorDeviceService.registerSensor(request);
    }

    @Test
    void createAndAcknowledgeCommand() {
        CreateSensorCommandRequest createRequest = new CreateSensorCommandRequest();
        createRequest.setSensorId(sensorId);
        createRequest.setCommandType(SensorCommandType.SYNC_TIME);
        createRequest.setCommandPayload("{\"timezone\":\"Asia/Seoul\"}");
        createRequest.setRequestedBy("admin@zeroq.kr");

        var created = sensorCommandService.createCommand(createRequest);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(SensorCommandStatus.PENDING);

        sensorCommandService.markSent(created.getId());

        AckSensorCommandRequest ackRequest = new AckSensorCommandRequest();
        ackRequest.setStatus(SensorCommandStatus.ACKNOWLEDGED);
        ackRequest.setAckPayload("{\"result\":\"ok\"}");

        var acked = sensorCommandService.acknowledgeCommand(created.getId(), ackRequest);

        assertThat(acked.getStatus()).isEqualTo(SensorCommandStatus.ACKNOWLEDGED);
        assertThat(acked.getAckPayload()).isEqualTo("{\"result\":\"ok\"}");
    }
}
