package com.minisiem.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "siem.log")
@Getter
@Setter
public class LogSourceConfig {

    private List<LogSource> sources;

    @Getter
    @Setter
    public static class LogSource {
        private String path;       // 로그 파일 경로 또는 디렉토리
        private String type;       // NGINX, TOMCAT, SYSLOG 등
        private String pattern;    // 파일 이름 패턴 (선택, 기본 *.log)
    }
}