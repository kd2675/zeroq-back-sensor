package com.zeroq.sensor.database.pub.repository;

import com.zeroq.sensor.database.pub.entity.SensorHeartbeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorHeartbeatRepository extends JpaRepository<SensorHeartbeat, Long> {
}
