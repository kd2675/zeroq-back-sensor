# zeroq-back-sensor

ZeroQ 오프라인 센서(ESP32/Gateway/MQTT) 데이터 수집 서버입니다.

## 역할
- 오프라인 기계 센서 텔레메트리/하트비트 수집
- 데이터 품질 판정(VALID/OUTLIER/STALE/DUPLICATE)
- 장소 단위 스냅샷 집계(`place_occupancy_snapshot`)
- 센서 제어 명령 생성/전송/ACK 처리
- 수집 실패 데이터 데드레터 관리

## 아키텍처 포인트
- 모든 외부 API는 Gateway 경유 전제 (`/api/zeroq/v1/sensor/**`)
- 센서 실시간 수집은 HTTP + MQTT 인바운드 모두 지원
- 명령 전송은 MQTT 아웃바운드 사용
- 센서 운영 API는 `X-User-Role` 기준 `MANAGER`/`ADMIN`만 허용

## 기본 포트
- `local/dev`: `20181`
- `prod`: `10181`
- `test`: `30181`

## 주요 API
- `POST /api/zeroq/v1/sensor/devices` 센서 등록
- `PUT /api/zeroq/v1/sensor/devices/{sensorId}/install` 설치 정보 반영
- `PATCH /api/zeroq/v1/sensor/devices/{sensorId}/status` 센서 상태 변경
- `POST /api/zeroq/v1/sensor/ingest/telemetry` 텔레메트리 수집
- `POST /api/zeroq/v1/sensor/ingest/heartbeat` 하트비트 수집
- `POST /api/zeroq/v1/sensor/ingest/batch` 게이트웨이 배치 수집
- `POST /api/zeroq/v1/sensor/commands` 센서 명령 생성
- `POST /api/zeroq/v1/sensor/commands/{commandId}/dispatch` 명령 MQTT 전송
- `PATCH /api/zeroq/v1/sensor/commands/{commandId}/ack` 명령 ACK 반영
- `GET /api/zeroq/v1/sensor/monitoring/places/{placeId}/snapshot` 스냅샷 조회
- `GET /api/zeroq/v1/sensor/monitoring/dead-letters` 실패 메시지 조회

## MQTT 토픽
- 텔레메트리 수신: `zeroq/sensor/+/telemetry`
- 하트비트 수신: `zeroq/sensor/+/heartbeat`
- 명령 ACK 수신: `zeroq/sensor/+/command-ack`
- 명령 발행: `zeroq/sensor/{sensorId}/command`

## 로그
- `logback-spring.xml` 적용
- 커스텀 클래스:
  - `com.zeroq.sensor.common.logback.PrintOnlyWarningLogbackStatusListener`
  - `com.zeroq.sensor.common.logback.filter.CustomLogbackFilter`
- 프로파일별 경로:
  - local/test: `./logs`
  - dev: `/data/logs/dev/zeroq_back_sensor`
  - prod: `/data/logs/prod/zeroq_back_sensor`

## 수집 안전장치
- 배치 수집은 항목별 독립 트랜잭션으로 처리(부분 성공 보장)
- 설치된 센서 `placeId`와 요청 `placeId` 불일치 시 수집 거부
- 중복 판정은 `sensorId + sequenceNo + measuredAt` 기준

## DB 스크립트
- DDL: `src/main/resources/db/ddl/zeroq_sensor_all.sql`
- 리셋: `src/main/resources/db/ddl/zeroq_sensor_data_reset.sql`
- Seed: `src/main/resources/db/seed/zeroq_sensor_seed.sql`

## 실행
```bash
./gradlew :zeroq-back-sensor:bootRun
```

## 빌드/테스트
```bash
./gradlew :zeroq-back-sensor:compileJava
./gradlew :zeroq-back-sensor:test
```
