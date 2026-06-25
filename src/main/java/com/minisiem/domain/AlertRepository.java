package com.minisiem.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByStatusOrderByCreatedAtDesc(Alert.AlertStatus status);

    List<Alert> findBySrcIpOrderByCreatedAtDesc(String srcIp);

    List<Alert> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    List<Alert> findBySeverityOrderByCreatedAtDesc(Alert.Severity severity);

    // 같은 룰 + 같은 IP + 윈도우가 겹치는 중복 경보 방지용 (커밋 10에서 사용)
    boolean existsByRuleIdAndSrcIpAndWindowStartGreaterThanEqual(
            Long ruleId, String srcIp, LocalDateTime windowStart
    );
}