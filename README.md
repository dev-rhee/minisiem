# Mini SIEM

스터디용 미니 SIEM입니다.
로그 수집 → 상관분석 → 대시보드 → 위협 인텔 연동까지 단계별로 구현합니다.

## 1단계 — 로그 수집 & 파싱

- Nginx 로그를 파싱해 정규화된 형태로 PostgreSQL에 저장
- Spring Batch로 30초 주기 수집, offset 기반 중복 방지
- Flyway로 스키마 버전 관리
- Swagger UI로 조회 API 문서화



## 기술 스택

Java 21, Spring Boot 3.3, Spring Batch, Spring Data JPA, PostgreSQL 15, Flyway, SpringDoc OpenAPI, Docker Compose

## 프로젝트 구조

\`\`\`
src/main/java/com/minisiem/
├── api/         REST 컨트롤러
├── collector/   로그 수집 (Batch Job, Scheduler)
├── domain/      엔티티, 리포지토리
└── parser/      로그 파서
\`\`\`


실행 후 http://localhost:8080/swagger-ui.html 에서 API 확인 가능합니다.