package com.minisiem.parser;

import com.minisiem.domain.LogEvent;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NginxLogParser {

    // Nginx combined log format
    // 192.168.1.1 - - [12/Jun/2026:10:00:01 +0900] "GET /api HTTP/1.1" 200 512 "-" "curl/7.0"
    private static final Pattern PATTERN = Pattern.compile(
            "^(\\S+)\\s+\\S+\\s+\\S+\\s+\\[([^\\]]+)\\]\\s+" +
                    "\"(\\w+)\\s+(\\S+)\\s+\\S+\"\\s+(\\d{3})\\s+(\\d+|-)(?:\\s+\"[^\"]*\"\\s+\"([^\"]*)\")?$"
    );

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    public Optional<LogEvent> parse(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }

        Matcher m = PATTERN.matcher(line.trim());
        if (!m.find()) {
            return Optional.empty();
        }

        return Optional.of(LogEvent.builder()
                .source(LogEvent.LogSource.NGINX)
                .srcIp(m.group(1))
                .occurredAt(ZonedDateTime.parse(m.group(2), FMT).toLocalDateTime())
                .method(m.group(3))
                .requestUri(m.group(4))
                .statusCode(Integer.parseInt(m.group(5)))
                .responseBytes("-".equals(m.group(6)) ? 0L : Long.parseLong(m.group(6)))
                .userAgent(m.group(7))
                .rawLog(line)
                .build());
    }
}