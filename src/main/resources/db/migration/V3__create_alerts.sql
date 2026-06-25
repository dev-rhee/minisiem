CREATE TABLE IF NOT EXISTS alerts (
                                      id                BIGSERIAL    PRIMARY KEY,
                                      rule_id           BIGINT       NOT NULL REFERENCES correlation_rules(id),
    rule_name         VARCHAR(100) NOT NULL,
    severity          VARCHAR(20)  NOT NULL,
    src_ip            VARCHAR(45),
    occurred_count    INT          NOT NULL,
    window_start      TIMESTAMP    NOT NULL,
    window_end        TIMESTAMP    NOT NULL,
    triggered_log_ids TEXT,
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at        TIMESTAMP    DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_alerts_src_ip      ON alerts(src_ip);
CREATE INDEX IF NOT EXISTS idx_alerts_created_at  ON alerts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_severity    ON alerts(severity);
CREATE INDEX IF NOT EXISTS idx_alerts_status      ON alerts(status);

COMMENT ON TABLE  alerts                    IS '상관분석 결과로 생성된 경보';
COMMENT ON COLUMN alerts.rule_id            IS '발동된 룰 ID (FK)';
COMMENT ON COLUMN alerts.occurred_count     IS '윈도우 내 실제 발생 건수';
COMMENT ON COLUMN alerts.triggered_log_ids  IS '연관된 log_events ID 목록 (콤마 구분)';
COMMENT ON COLUMN alerts.status             IS 'OPEN | ACKNOWLEDGED | RESOLVED';