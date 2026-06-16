CREATE TABLE IF NOT EXISTS log_events (
                                          id             BIGSERIAL    PRIMARY KEY,
                                          source         VARCHAR(20)  NOT NULL,
    src_ip         VARCHAR(45),
    occurred_at    TIMESTAMP    NOT NULL,
    method         VARCHAR(10),
    request_uri    VARCHAR(2048),
    status_code    INT,
    response_bytes BIGINT,
    user_agent     VARCHAR(512),
    raw_log        TEXT,
    created_at     TIMESTAMP    DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_log_src_ip
    ON log_events(src_ip);

CREATE INDEX IF NOT EXISTS idx_log_occurred_at
    ON log_events(occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_log_status
    ON log_events(status_code);

COMMENT ON TABLE  log_events               IS '수집된 로그 이벤트';
COMMENT ON COLUMN log_events.source        IS 'NGINX | TOMCAT | SYSLOG';
COMMENT ON COLUMN log_events.occurred_at   IS '로그 원본 발생 시각';
COMMENT ON COLUMN log_events.raw_log       IS '파싱 전 원본 로그 라인';