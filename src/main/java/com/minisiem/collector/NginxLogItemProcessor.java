package com.minisiem.collector;

import com.minisiem.domain.LogEvent;
import com.minisiem.parser.NginxLogParser;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NginxLogItemProcessor implements ItemProcessor<String, LogEvent> {

    private final NginxLogParser parser;

    @Override
    public LogEvent process(String line) {
        return parser.parse(line)
                .orElseThrow(() -> new LogParseException("파싱 실패: " + line));
    }
}
