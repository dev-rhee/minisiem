package com.minisiem.correlation;

import com.minisiem.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEvaluatorTest {

    @Mock private CorrelationRuleRepository ruleRepository;
    @Mock private LogEventRepository logEventRepository;
    @Mock private AlertRepository alertRepository;

    @InjectMocks
    private RuleEvaluator ruleEvaluator;

    private CorrelationRule sameIpRule;

    @BeforeEach
    void setUp() {
        sameIpRule = CorrelationRule.builder()
                .id(1L)
                .ruleName("동일 IP 다발성 요청")
                .conditionType(CorrelationRule.ConditionType.SAME_IP_COUNT)
                .threshold(10)
                .windowSeconds(300)
                .severity(CorrelationRule.Severity.MEDIUM)
                .enabled(true)
                .build();
    }

    @Test
    void 임계치_초과_IP는_경보가_생성된다() {
        LogEventRepository.IpCountResult result = mock(LogEventRepository.IpCountResult.class);
        when(result.getSrcIp()).thenReturn("192.168.1.1");
        when(result.getCnt()).thenReturn(15L);

        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(sameIpRule));
        when(logEventRepository.findIpsExceedingThreshold(any(), any(), eq(10L)))
                .thenReturn(List.of(result));
        when(alertRepository.existsByRuleIdAndSrcIpAndWindowStartGreaterThanEqual(any(), any(), any()))
                .thenReturn(false);
        when(logEventRepository.findIdsBySrcIpAndOccurredAtBetween(any(), any(), any()))
                .thenReturn(List.of(1L, 2L, 3L));

        ruleEvaluator.evaluateAll();

        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void 이미_경보가_있으면_중복_생성되지_않는다() {
        LogEventRepository.IpCountResult result = mock(LogEventRepository.IpCountResult.class);

        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(sameIpRule));
        when(logEventRepository.findIpsExceedingThreshold(any(), any(), eq(10L)))
                .thenReturn(List.of(result));
        when(alertRepository.existsByRuleIdAndSrcIpAndWindowStartGreaterThanEqual(any(), any(), any()))
                .thenReturn(true);

        ruleEvaluator.evaluateAll();

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void 비활성화된_룰은_평가되지_않는다() {
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of());

        ruleEvaluator.evaluateAll();

        verifyNoInteractions(logEventRepository);
        verifyNoInteractions(alertRepository);
    }
}