package com.minisiem.collector;

import com.minisiem.domain.LogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogCollectSkipListener implements SkipListener<String, LogEvent> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("[Skip][Read] 읽기 중 예외 발생 — {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(String item, Throwable t) {
        log.warn("[Skip][Process] 파싱 실패 — 원본 줄: {}", item);
    }

    @Override
    public void onSkipInWrite(LogEvent item, Throwable t) {
        log.warn("[Skip][Write] 저장 실패 — source={}, occurredAt={} — {}",
                item.getSource(), item.getOccurredAt(), t.getMessage());
    }
}
