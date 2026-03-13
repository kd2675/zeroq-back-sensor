package com.zeroq.sensor.database.pub.repository;

import com.zeroq.sensor.database.pub.entity.GatewayHeartbeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GatewayHeartbeatRepository extends JpaRepository<GatewayHeartbeat, Long> {
}
