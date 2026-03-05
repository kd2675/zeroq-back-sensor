package com.zeroq.sensor.service.monitoring.vo;

import com.zeroq.sensor.database.pub.entity.PlaceOccupancySnapshot;
import com.zeroq.sensor.database.pub.entity.SnapshotCrowdLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PlaceSnapshotResponse {
    private Long placeId;
    private int occupiedCount;
    private int activeSensorCount;
    private double occupancyRate;
    private SnapshotCrowdLevel crowdLevel;
    private LocalDateTime lastMeasuredAt;
    private LocalDateTime lastCalculatedAt;
    private int sourceWindowSeconds;

    public static PlaceSnapshotResponse from(PlaceOccupancySnapshot snapshot) {
        return PlaceSnapshotResponse.builder()
                .placeId(snapshot.getPlaceId())
                .occupiedCount(snapshot.getOccupiedCount())
                .activeSensorCount(snapshot.getActiveSensorCount())
                .occupancyRate(snapshot.getOccupancyRate())
                .crowdLevel(snapshot.getCrowdLevel())
                .lastMeasuredAt(snapshot.getLastMeasuredAt())
                .lastCalculatedAt(snapshot.getLastCalculatedAt())
                .sourceWindowSeconds(snapshot.getSourceWindowSeconds())
                .build();
    }
}
