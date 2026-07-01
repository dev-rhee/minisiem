package com.minisiem.correlation;

import com.minisiem.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RuleEvaluator {

    private final CorrelationRuleRepository ruleRepository;
    private final LogEventRepository logEventRepository;
    private final AlertRepository alertRepository;
    private final AlertEventPublisher alertEventPublisher;

    @Transactional
    public void evaluateAll() {
        List<CorrelationRule> activeRules = ruleRepository.findByEnabledTrue();

        for (CorrelationRule rule : activeRules) {
            try {
                evaluateRule(rule);
            } catch (Exception e) {
                log.error("룰 평가 실패 - ruleId: {}, ruleName: {}", rule.getId(), rule.getRuleName(), e);
            }
        }
    }

    private void evaluateRule(CorrelationRule rule) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusSeconds(rule.getWindowSeconds());

        switch (rule.getConditionType()) {
            case SAME_IP_COUNT -> evaluateSameIpCount(rule, windowStart, now);
            case STATUS_CODE_COUNT -> evaluateStatusCodeCount(rule, windowStart, now);
            case SAME_IP_MULTI_STATUS -> log.debug("SAME_IP_MULTI_STATUS는 후에 구현 예정");
        }
    }

    private void evaluateSameIpCount(CorrelationRule rule, LocalDateTime from, LocalDateTime to) {
        List<LogEventRepository.IpCountResult> results =
                logEventRepository.findIpsExceedingThreshold(from, to, rule.getThreshold());

        for (LogEventRepository.IpCountResult result : results) {
            // 같은 윈도우 내 중복 경보 방지
            if (alertRepository.existsByRuleIdAndSrcIpAndWindowStartGreaterThanEqual(
                    rule.getId(), result.getSrcIp(), from)) {
                continue;
            }

            List<Long> logIds = logEventRepository.findIdsBySrcIpAndOccurredAtBetween(
                    result.getSrcIp(), from, to
            );

            saveAlert(rule, result.getSrcIp(), result.getCnt().intValue(), from, to, logIds);
        }
    }

    private void evaluateStatusCodeCount(CorrelationRule rule, LocalDateTime from, LocalDateTime to) {
        // 404를 기본 대상으로 단순화
        long count = logEventRepository.countByStatusCodeAndOccurredAtBetween(404, from, to);

        if (count >= rule.getThreshold()) {
            saveAlert(rule, null, (int) count, from, to, List.of());
        }
    }

    private void saveAlert(
            CorrelationRule rule, String srcIp, int occurredCount,
            LocalDateTime windowStart, LocalDateTime windowEnd, List<Long> logIds
    ) {
        Alert alert = Alert.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getRuleName())
                .severity(rule.getSeverity())
                .srcIp(srcIp)
                .occurredCount(occurredCount)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .triggeredLogIds(logIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();

        Alert saved = alertRepository.save(alert);
        alertEventPublisher.publish(saved); // 추가 — 저장 후 바로 push
        log.info("경보 발생 - 룰: {}, IP: {}, 건수: {}", rule.getRuleName(), srcIp, occurredCount);
    }

}