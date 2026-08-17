# Mini SIEM

스터디용 미니 SIEM입니다.
로그 수집 → 상관분석 → 경보 스트리밍 → 대시보드까지 단계별로 구현합니다.

## 1단계 — 로그 수집 & 파싱

- Nginx 로그를 정규화된 형태로 PostgreSQL에 저장
- Spring Batch로 30초 주기 수집, offset 기반 중복 방지
- LogParser 인터페이스로 다중 로그 소스 지원 (설정만으로 파서 교체 가능)
- Chunk 방식(ItemReader → Processor → Writer)으로 skip/retry 처리
- Flyway로 스키마 버전 관리

## 2단계 — 상관분석 룰 엔진

- DB에 저장된 룰(임계값, 시간 윈도우)을 30초 주기로 평가
- 조건 충족 시 경보(Alert) 자동 생성
- 심각도(LOW / MEDIUM / HIGH / CRITICAL) 분류

## 3단계 — 실시간 경보 스트리밍

- SSE(Server-Sent Events)로 경보 발생 즉시 클라이언트에 Push
- AlertEventPublisher → AlertStreamController 구조로 Spring 이벤트 기반 전달

## 4단계 — React 대시보드

- 실시간 경보 피드 및 심각도별 통계
- Top 5 공격 IP 차트
- SSE 연결로 새 경보 자동 반영

## 기술 스택

Java 21, Spring Boot 3.5, Spring Batch, Spring Data JPA, PostgreSQL 15, Flyway, SpringDoc OpenAPI, Docker Compose, React

## 프로젝트 구조

```
src/main/java/com/minisiem/
├── api/          REST 컨트롤러 (로그 조회, 경보, SSE 스트림)
├── collector/    로그 수집 (Batch Job, Scheduler, Chunk 구성요소)
├── config/       설정 (로그 소스, CORS)
├── correlation/  상관분석 룰 엔진
├── domain/       엔티티, 리포지토리
└── parser/       로그 파서 (LogParser 인터페이스, Nginx 구현체)

frontend/src/
├── components/   TopIpChart, AlertStats
└── App.js        SSE 실시간 연결
```

## 실행

```bash
docker compose up -d
./gradlew bootRun
```

실행 후 http://localhost:8080/swagger-ui.html 에서 API 확인 가능합니다.
