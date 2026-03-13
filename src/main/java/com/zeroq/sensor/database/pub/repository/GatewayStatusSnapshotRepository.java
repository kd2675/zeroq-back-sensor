package com.zeroq.sensor.database.pub.repository;

import com.zeroq.sensor.database.pub.entity.GatewayStatusSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GatewayStatusSnapshotRepository extends JpaRepository<GatewayStatusSnapshot, Long> {
    Optional<GatewayStatusSnapshot> findByGatewayId(String gatewayId);
}
