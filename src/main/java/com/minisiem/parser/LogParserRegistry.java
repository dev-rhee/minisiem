package com.minisiem.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LogParserRegistry {

    private final Map<String, LogParser> parserMap;

    public LogParserRegistry(List<LogParser> parsers) {
        this.parserMap = parsers.stream()
                .collect(Collectors.toMap(
                        LogParser::getSupportedType,
                        Function.identity()
                ));
        log.info("등록된 파서: {}", parserMap.keySet());
    }

    public Optional<LogParser> getParser(String type) {
        LogParser parser = parserMap.get(type.toUpperCase());
        if (parser == null) {
            log.warn("지원하지 않는 로그 타입: {}", type);
        }
        return Optional.ofNullable(parser);
    }
}