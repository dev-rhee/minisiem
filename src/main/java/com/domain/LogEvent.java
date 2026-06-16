package com.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_events")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
