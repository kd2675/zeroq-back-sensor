# zeroq-back-sensor

ZeroQ 센서 수집 및 제어 서버입니다. HTTP와 MQTT를 통해 센서 데이터를 받아 정제하고, 명령 생성/전송/ACK, 스냅샷 집계, 데드레터 관리를 수행합니다.

## 역할

- 센서 장치 등록과 설치 상태 관리
- telemetry, heartbeat, batch ingest 처리
- 품질 판정과 장소 스냅샷 집계
- 센서 명령 생성, MQTT dispatch, ACK 반영
- dead-letter 조회와 최근 수집 모니터링

## API 베이스 경로

- `/api/zeroq/v1/sensor/devices`
- `/api/zeroq/v1/sensor/ingest`
- `/api/zeroq/v1/sensor/commands`
- `/api/zeroq/v1/sensor/monitoring`
- 사용자 공개 스냅샷: `/api/zeroq/v1/sensor/monitoring/places/{placeId}/snapshot/public`

## MQTT

- telemetry: `zeroq/sensor/+/telemetry`
- heartbeat: `zeroq/sensor/+/heartbeat`
- command ack: `zeroq/sensor/+/command-ack`
- command publish pattern: `zeroq/sensor/{sensorId}/command`

## 실행 프로필과 포트

| Profile | Port |
|---|---:|
| `local` | `20181` |
| `dev` | `20181` |
| `prod` | `10181` |
| `test` | `30181` |

## 실행과 검증

```bash
./gradlew :zeroq-back-sensor:bootRun
./gradlew :zeroq-back-sensor:bootRun --args='--spring.profiles.active=local'
./gradlew :zeroq-back-sensor:compileJava
./gradlew :zeroq-back-sensor:test
```

## 핵심 설정

- DB: `SENSOR_DB_URL`, `SENSOR_DB_USERNAME`, `SENSOR_DB_PASSWORD`
- MQTT 사용 여부: `SENSOR_MQTT_ENABLED`
- MQTT broker: `SENSOR_MQTT_BROKER_URI`
- CORS: `SENSOR_CORS_ALLOWED_ORIGINS`

## 데이터와 로그

- DDL: `src/main/resources/db/ddl/zeroq_sensor_all.sql`
- 리셋: `src/main/resources/db/ddl/zeroq_sensor_data_reset.sql`
- 시드: `src/main/resources/db/seed/zeroq_sensor_seed.sql`
- 로그 설정: `src/main/resources/logback-spring.xml`
- 로그 경로: local/test `./logs`, dev `/data/logs/dev/zeroq_back_sensor`, prod `/data/logs/prod/zeroq_back_sensor`

## 참고

- 운영 API는 `MANAGER`/`ADMIN` 권한 전제를 둡니다.
- 공개 스냅샷 경로는 인증된 사용자 역할(`USER`/`MANAGER`/`ADMIN`)이면 조회할 수 있습니다.
- 배치 수집은 항목별 독립 트랜잭션으로 처리됩니다.
- 현재 테스트는 존재하지만 전반적으로 얕은 편이라 핵심 로직 변경 시 회귀 테스트 보강이 필요합니다.
