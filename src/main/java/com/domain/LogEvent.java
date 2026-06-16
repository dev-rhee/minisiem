package com.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_events", indexes = {

    @Index(name = "idx_log_src_ip", columnList = "src_ip"),
    @Index(name = "idx_log_occurred_at", columnList = "occurred_at"),
    @Index(name = "idx_log_status", columnList = "status_code")

})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogSource source;

    @Column(name = "src_ip", length = 45)
    private String srcIp;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(length = 10)
    private String method;

    @Column(name = "request_uri", length = 2048)
    private String requestUri;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_bytes")
    private Long responseBytes;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "raw_log", columnDefinition = "TEXT")
    private String rawLog;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum LogSource {
        NGINX, TOMCAT, SYSLOG
    }
}
