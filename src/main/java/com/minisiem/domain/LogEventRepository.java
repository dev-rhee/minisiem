package com.minisiem.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // SAME_IP_COUNT 룰용 — 윈도우 내 IP별 발생 건수 집계
    @Query("""
        SELECT l.srcIp AS srcIp, COUNT(l) AS cnt
        FROM LogEvent l
        WHERE l.occurredAt BETWEEN :from AND :to
          AND l.srcIp IS NOT NULL
        GROUP BY l.srcIp
        HAVING COUNT(l) >= :threshold
        """)
    List<IpCountResult> findIpsExceedingThreshold(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("threshold") long threshold
    );

    // STATUS_CODE_COUNT 룰용 — 윈도우 내 특정 상태코드 발생 건수
    long countByStatusCodeAndOccurredAtBetween(Integer statusCode, LocalDateTime from, LocalDateTime to);

    // 경보에 연결할 로그 ID 목록 조회
    @Query("""
        SELECT l.id FROM LogEvent l
        WHERE l.srcIp = :srcIp AND l.occurredAt BETWEEN :from AND :to
        """)
    List<Long> findIdsBySrcIpAndOccurredAtBetween(
            @Param("srcIp") String srcIp,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    interface IpCountResult {
        String getSrcIp();
        Long getCnt();
    }
}