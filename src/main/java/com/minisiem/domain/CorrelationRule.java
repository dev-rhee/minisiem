package com.minisiem.domain;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "correlation_rules")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrelationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 30)
    private ConditionType conditionType;

    @Column(nullable = false)
    private Integer threshold;

    @Column(name = "window_seconds", nullable = false)
    private Integer windowSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.enabled == null) this.enabled = true;
    }

    public enum ConditionType {
        SAME_IP_COUNT,        // 동일 IP 요청 건수
        STATUS_CODE_COUNT,    // 특정 상태코드 발생 건수
        SAME_IP_MULTI_STATUS  // 동일 IP에서 여러 상태코드 패턴
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}