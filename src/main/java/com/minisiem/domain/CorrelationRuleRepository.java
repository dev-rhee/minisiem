package com.minisiem.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CorrelationRuleRepository extends JpaRepository<CorrelationRule, Long> {
    List<CorrelationRule> findByEnabledTrue();
}