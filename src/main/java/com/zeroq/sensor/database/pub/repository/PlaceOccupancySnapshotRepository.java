package com.zeroq.sensor.database.pub.repository;

import com.zeroq.sensor.database.pub.entity.PlaceOccupancySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceOccupancySnapshotRepository extends JpaRepository<PlaceOccupancySnapshot, Long> {
    Optional<PlaceOccupancySnapshot> findByPlaceId(Long placeId);
}
