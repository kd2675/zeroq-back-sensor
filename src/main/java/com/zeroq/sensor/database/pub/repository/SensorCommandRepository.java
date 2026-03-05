package com.zeroq.sensor.database.pub.repository;

import com.zeroq.sensor.database.pub.entity.SensorCommand;
import com.zeroq.sensor.database.pub.entity.SensorCommandStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SensorCommandRepository extends JpaRepository<SensorCommand, Long> {
    List<SensorCommand> findAllBySensorDeviceSensorIdAndStatusInOrderByRequestedAtAsc(
            String sensorId,
            Collection<SensorCommandStatus> statuses
    );
}
