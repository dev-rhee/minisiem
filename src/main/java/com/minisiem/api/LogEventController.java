package com.minisiem.api;

import com.minisiem.domain.LogEvent;
import com.minisiem.domain.LogEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Tag(name = "Log Events", description = "수집된 로그 이벤트 조회 API")
public class LogEventController {

    private final LogEventRepository logEventRepository;

    @GetMapping
    @Operation(summary = "전체 로그 조회", description = "시간 범위로 로그를 조회합니다. 범위를 지정하지 않으면 전체를 반환합니다.")
    public ResponseEntity<List<LogEvent>> getLogs(
            @Parameter(description = "시작 시각 (ISO 8601)", example = "2026-06-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(description = "종료 시각 (ISO 8601)", example = "2026-06-30T23:59:59")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        if (from != null && to != null) {
            return ResponseEntity.ok(
                    logEventRepository.findByOccurredAtBetweenOrderByOccurredAtDesc(from, to)
            );
        }
        return ResponseEntity.ok(logEventRepository.findAll());
    }

    @GetMapping("/ip/{srcIp}")
    @Operation(summary = "IP별 로그 조회", description = "특정 소스 IP의 로그를 최신순으로 조회합니다.")
    public ResponseEntity<List<LogEvent>> getByIp(
            @Parameter(description = "소스 IP 주소", example = "192.168.1.1")
            @PathVariable String srcIp
    ) {
        return ResponseEntity.ok(
                logEventRepository.findBySrcIpOrderByOccurredAtDesc(srcIp)
        );
    }

    @GetMapping("/status/{code}")
    @Operation(summary = "상태코드별 로그 조회", description = "HTTP 상태코드로 로그를 필터링합니다.")
    public ResponseEntity<List<LogEvent>> getByStatus(
            @Parameter(description = "HTTP 상태코드", example = "404")
            @PathVariable Integer code
    ) {
        return ResponseEntity.ok(
                logEventRepository.findByStatusCodeOrderByOccurredAtDesc(code)
        );
    }

    @GetMapping("/stats/top-ips")
    @Operation(summary = "Top IP 통계", description = "요청 수 기준 상위 10개 IP를 반환합니다.")
    public ResponseEntity<List<Map<String, Object>>> getTopIps() {
        Map<String, Long> counts = logEventRepository.findAll().stream()
                .filter(e -> e.getSrcIp() != null)
                .collect(Collectors.groupingBy(LogEvent::getSrcIp, Collectors.counting()));

        List<Map<String, Object>> result = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of("ip", e.getKey(), "count", e.getValue()))
                .toList();

        return ResponseEntity.ok(result);
    }
}