package com.minisiem.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LogEventRepository extends JpaRepository<LogEvent, Long> {

    // IP로 조회
    List<LogEvent> findBySrcIpOrderByOccurredAtDesc(String srcIp);

    // 시간 범위 조회
    List<LogEvent> findByOccurredAtBetweenOrderByOccurredAtDesc(
            LocalDateTime from, LocalDateTime to
    );

    // 상태코드로 조회
    List<LogEvent> findByStatusCodeOrderByOccurredAtDesc(Integer statusCode);

    // IP + 시간 범위 내 건수 (2단계 상관분석에서 사용)
    long countBySrcIpAndOccurredAtBetween(
            String srcIp, LocalDateTime from, LocalDateTime to
    );
}