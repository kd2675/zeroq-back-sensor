package com.zeroq.sensor.database.pub.entity;

import com.zeroq.sensor.common.jpa.CommonDateEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "place_occupancy_snapshot", indexes = {
        @Index(name = "idx_place_occupancy_snapshot_place", columnList = "place_id", unique = true),
        @Index(name = "idx_place_occupancy_snapshot_calculated", columnList = "last_calculated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOccupancySnapshot extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false, unique = true)
    private Long placeId;

    @Column(name = "occupied_count", nullable = false)
    @Builder.Default
    private int occupiedCount = 0;

    @Column(name = "active_sensor_count", nullable = false)
    @Builder.Default
    private int activeSensorCount = 0;

    @Column(name = "occupancy_rate", nullable = false)
    @Builder.Default
    private double occupancyRate = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "crowd_level", nullable = false, length = 20)
    @Builder.Default
    private SnapshotCrowdLevel crowdLevel = SnapshotCrowdLevel.EMPTY;

    @Column(name = "last_measured_at")
    private LocalDateTime lastMeasuredAt;

    @Column(name = "last_calculated_at", nullable = false)
    private LocalDateTime lastCalculatedAt;

    @Column(name = "source_window_seconds", nullable = false)
    @Builder.Default
    private int sourceWindowSeconds = 300;
}
