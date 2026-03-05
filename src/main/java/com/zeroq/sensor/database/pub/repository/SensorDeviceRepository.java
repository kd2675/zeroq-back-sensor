package com.zeroq.sensor.database.pub.repository;

import com.zeroq.sensor.database.pub.entity.SensorDevice;
import com.zeroq.sensor.database.pub.entity.SensorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SensorDeviceRepository extends JpaRepository<SensorDevice, Long> {
    Optional<SensorDevice> findBySensorId(String sensorId);

    Optional<SensorDevice> findByMacAddress(String macAddress);

    boolean existsBySensorId(String sensorId);

    boolean existsByMacAddress(String macAddress);

    List<SensorDevice> findAllByPlaceId(Long placeId);

    List<SensorDevice> findAllByPlaceIdAndStatusIn(Long placeId, Collection<SensorStatus> statuses);
}
