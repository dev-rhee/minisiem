CREATE TABLE IF NOT EXISTS correlation_rules (
                                                 id              BIGSERIAL    PRIMARY KEY,
                                                 rule_name       VARCHAR(100) NOT NULL,
    condition_type  VARCHAR(30)  NOT NULL,
    threshold       INT          NOT NULL,
    window_seconds  INT          NOT NULL,
    severity        VARCHAR(20)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT true,
    description     VARCHAR(500),
    created_at      TIMESTAMP    DEFAULT NOW()
    );

COMMENT ON TABLE  correlation_rules                IS '상관분석 룰 정의';
COMMENT ON COLUMN correlation_rules.condition_type IS 'SAME_IP_COUNT | STATUS_CODE_COUNT | SAME_IP_MULTI_STATUS';
COMMENT ON COLUMN correlation_rules.threshold       IS '윈도우 내 발생 임계 건수';
COMMENT ON COLUMN correlation_rules.window_seconds  IS '집계 시간 윈도우(초)';
COMMENT ON COLUMN correlation_rules.severity        IS 'LOW | MEDIUM | HIGH | CRITICAL';

-- 초기 룰 2개 삽입
INSERT INTO correlation_rules (rule_name, condition_type, threshold, window_seconds, severity, description) VALUES
                                                                                                                ('동일 IP 다발성 요청', 'SAME_IP_COUNT', 10, 300, 'MEDIUM', '5분 내 동일 IP에서 10회 이상 요청 발생'),
                                                                                                                ('404 다발성 발생', 'STATUS_CODE_COUNT', 20, 60, 'LOW', '1분 내 404 응답 20회 이상 (스캐닝 의심)');