package com.minisiem.api;

import com.minisiem.domain.Alert;
import com.minisiem.domain.AlertRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "상관분석으로 생성된 경보 조회 API")
public class AlertController {

    private final AlertRepository alertRepository;

    @GetMapping
    @Operation(summary = "전체 경보 조회", description = "시간 범위로 경보를 조회합니다. 범위를 지정하지 않으면 전체를 반환합니다.")
    public ResponseEntity<List<Alert>> getAlerts(
            @Parameter(description = "시작 시각 (ISO 8601)", example = "2026-06-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(description = "종료 시각 (ISO 8601)", example = "2026-06-30T23:59:59")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        if (from != null && to != null) {
            return ResponseEntity.ok(
                    alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to)
            );
        }
        return ResponseEntity.ok(alertRepository.findAll());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "상태별 경보 조회", description = "OPEN, ACKNOWLEDGED, RESOLVED 상태로 필터링합니다.")
    public ResponseEntity<List<Alert>> getByStatus(
            @Parameter(description = "경보 상태", example = "OPEN")
            @PathVariable Alert.AlertStatus status
    ) {
        return ResponseEntity.ok(
                alertRepository.findByStatusOrderByCreatedAtDesc(status)
        );
    }

    @GetMapping("/ip/{srcIp}")
    @Operation(summary = "IP별 경보 조회", description = "특정 IP에서 발생한 경보를 최신순으로 조회합니다.")
    public ResponseEntity<List<Alert>> getByIp(
            @Parameter(description = "소스 IP 주소", example = "192.168.1.99")
            @PathVariable String srcIp
    ) {
        return ResponseEntity.ok(
                alertRepository.findBySrcIpOrderByCreatedAtDesc(srcIp)
        );
    }

    @GetMapping("/severity/{severity}")
    @Operation(summary = "심각도별 경보 조회", description = "LOW, MEDIUM, HIGH, CRITICAL로 필터링합니다.")
    public ResponseEntity<List<Alert>> getBySeverity(
            @Parameter(description = "심각도", example = "MEDIUM")
            @PathVariable com.minisiem.domain.CorrelationRule.Severity severity
    ) {
        return ResponseEntity.ok(
                alertRepository.findBySeverityOrderByCreatedAtDesc(severity)
        );
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "경보 상태 변경", description = "경보를 ACKNOWLEDGED 또는 RESOLVED로 변경합니다.")
    public ResponseEntity<Alert> updateStatus(
            @PathVariable Long id,
            @Parameter(description = "변경할 상태", example = "ACKNOWLEDGED")
            @RequestParam Alert.AlertStatus status
    ) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("경보를 찾을 수 없습니다: " + id));

        Alert updated = Alert.builder()
                .id(alert.getId())
                .ruleId(alert.getRuleId())
                .ruleName(alert.getRuleName())
                .severity(alert.getSeverity())
                .srcIp(alert.getSrcIp())
                .occurredCount(alert.getOccurredCount())
                .windowStart(alert.getWindowStart())
                .windowEnd(alert.getWindowEnd())
                .triggeredLogIds(alert.getTriggeredLogIds())
                .status(status)
                .createdAt(alert.getCreatedAt())
                .build();

        return ResponseEntity.ok(alertRepository.save(updated));
    }
}