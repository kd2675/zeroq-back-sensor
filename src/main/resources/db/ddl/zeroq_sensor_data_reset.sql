-- Reset ZeroQ sensor raw data only.
-- Safe to run multiple times.

USE ZEROQ_SENSOR;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE sensor_dead_letter;
TRUNCATE TABLE sensor_heartbeat;
TRUNCATE TABLE sensor_command;
TRUNCATE TABLE sensor_telemetry;

SET FOREIGN_KEY_CHECKS = 1;
