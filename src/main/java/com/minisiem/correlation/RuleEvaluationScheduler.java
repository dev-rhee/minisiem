package com.minisiem.correlation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RuleEvaluationScheduler {

    private final RuleEvaluator ruleEvaluator;

    @Scheduled(fixedDelay = 60_000) // 1분마다
    public void run() {
        log.debug("상관분석 룰 평가 시작");
        ruleEvaluator.evaluateAll();
        log.debug("상관분석 룰 평가 완료");
    }
}