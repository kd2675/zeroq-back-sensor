package com.zeroq.sensor.database.pub.repository;

import com.zeroq.sensor.database.pub.entity.SensorDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorDeadLetterRepository extends JpaRepository<SensorDeadLetter, Long> {
    List<SensorDeadLetter> findTop100ByOrderByOccurredAtDesc();
}
