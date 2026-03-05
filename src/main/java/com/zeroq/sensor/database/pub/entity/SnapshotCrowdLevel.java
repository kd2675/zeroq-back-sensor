package com.zeroq.sensor.database.pub.entity;

public enum SnapshotCrowdLevel {
    EMPTY,
    LOW,
    MEDIUM,
    HIGH,
    FULL;

    public static SnapshotCrowdLevel fromOccupancyRate(double occupancyRate) {
        if (occupancyRate <= 10.0) {
            return EMPTY;
        }
        if (occupancyRate <= 35.0) {
            return LOW;
        }
        if (occupancyRate <= 65.0) {
            return MEDIUM;
        }
        if (occupancyRate <= 85.0) {
            return HIGH;
        }
        return FULL;
    }
}
