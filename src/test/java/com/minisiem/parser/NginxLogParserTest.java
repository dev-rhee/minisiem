package com.minisiem.parser;

import com.minisiem.domain.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NginxLogParserTest {

    private NginxLogParser parser;

    @BeforeEach
    void setUp() {
        parser = new NginxLogParser();
    }

    @Test
    @DisplayName("정상 Nginx 로그 파싱 성공")
    void parse_success() {
        String line = "192.168.1.1 - - [12/Jun/2026:10:00:01 +0900] " +
                "\"GET /api/users HTTP/1.1\" 200 512 \"-\" \"curl/7.0\"";

        Optional<LogEvent> result = parser.parse(line);

        assertThat(result).isPresent();
        assertThat(result.get().getSrcIp()).isEqualTo("192.168.1.1");
        assertThat(result.get().getMethod()).isEqualTo("GET");
        assertThat(result.get().getRequestUri()).isEqualTo("/api/users");
        assertThat(result.get().getStatusCode()).isEqualTo(200);
        assertThat(result.get().getResponseBytes()).isEqualTo(512L);
        assertThat(result.get().getSource()).isEqualTo(LogEvent.LogSource.NGINX);
    }

    @Test
    @DisplayName("404 상태코드 로그 파싱 성공")
    void parse_404() {
        String line = "10.0.0.1 - - [12/Jun/2026:11:00:00 +0900] " +
                "\"POST /login HTTP/1.1\" 404 0 \"-\" \"Mozilla/5.0\"";

        Optional<LogEvent> result = parser.parse(line);

        assertThat(result).isPresent();
        assertThat(result.get().getStatusCode()).isEqualTo(404);
        assertThat(result.get().getMethod()).isEqualTo("POST");
    }

    @Test
    @DisplayName("응답 바이트가 - 인 경우 0으로 처리")
    void parse_dash_bytes() {
        String line = "192.168.1.1 - - [12/Jun/2026:10:00:01 +0900] " +
                "\"GET /favicon.ico HTTP/1.1\" 304 - \"-\" \"-\"";

        Optional<LogEvent> result = parser.parse(line);

        assertThat(result).isPresent();
        assertThat(result.get().getResponseBytes()).isEqualTo(0L);
    }

    @Test
    @DisplayName("빈 문자열 파싱 시 빈 Optional 반환")
    void parse_empty_line() {
        Optional<LogEvent> result = parser.parse("");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null 파싱 시 빈 Optional 반환")
    void parse_null() {
        Optional<LogEvent> result = parser.parse(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("형식이 다른 로그는 빈 Optional 반환")
    void parse_invalid_format() {
        Optional<LogEvent> result = parser.parse("잘못된 로그 형식");
        assertThat(result).isEmpty();
    }
}