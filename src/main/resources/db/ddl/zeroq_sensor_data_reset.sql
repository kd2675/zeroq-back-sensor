-- Reset ZeroQ sensor domain data only
-- Safe to run multiple times

USE ZEROQ_SENSOR;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE sensor_dead_letter;
TRUNCATE TABLE sensor_command;
TRUNCATE TABLE place_occupancy_snapshot;
TRUNCATE TABLE sensor_telemetry;
TRUNCATE TABLE sensor_device;

SET FOREIGN_KEY_CHECKS = 1;
