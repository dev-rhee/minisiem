package com.minisiem.collector;

import com.minisiem.domain.LogEvent;
import com.minisiem.domain.LogEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogEventItemWriter implements ItemWriter<LogEvent> {

    private final LogEventRepository logEventRepository;

    @Override
    public void write(Chunk<? extends LogEvent> chunk) {
        logEventRepository.saveAll(chunk.getItems());
        log.info("{}건 저장 완료", chunk.size());
    }
}
