package ru.student.testing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.student.testing.dto.AlertEventDto;
import ru.student.testing.dto.AlertRuleDto;
import ru.student.testing.entity.AlertEvent;
import ru.student.testing.entity.AlertRule;
import ru.student.testing.repository.AlertEventRepository;
import ru.student.testing.repository.AlertRuleRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertEventRepository alertEventRepository;
    private final MetricService metricService;

    private final Map<String, Double> lastMetricValues = new HashMap<>();
    private final Map<String, Integer> violationCounter = new HashMap<>();

    @Transactional
    public void checkAlerts(Map<String, Double> currentMetrics) {
        List<AlertRule> activeRules = alertRuleRepository.findAllByIsActiveTrue();

        for (AlertRule rule : activeRules) {
            Double currentValue = currentMetrics.get(rule.getMetricName());
            if (currentValue == null) {
                continue;
            }

            boolean isViolated = rule.isViolated(currentValue);

            List<AlertEvent> activeEvents = alertEventRepository.findAllByRuleIdAndStatus(
                    rule.getId(), "triggered"
            );

            if (isViolated) {
                handleViolation(rule, currentValue, activeEvents);
            } else {
                handleNormalState(rule, currentValue, activeEvents);
            }
        }
    }

    private void handleViolation(AlertRule rule, Double currentValue, List<AlertEvent> activeEvents) {
        String key = rule.getId().toString();

        if (rule.getDurationSeconds() > 0) {
            violationCounter.merge(key, 1, Integer::sum);
            int requiredCount = rule.getDurationSeconds() / 5;

            if (violationCounter.get(key) < requiredCount) {
                return;
            }
        }

        if (activeEvents.isEmpty()) {
            AlertEvent event = new AlertEvent(rule, currentValue);
            alertEventRepository.save(event);
            log.warn("Алерт сработал! Правило: {}, Значение: {}",
                    rule.getName(), currentValue);
        }
        else {
            AlertEvent event = activeEvents.get(0);
            event.setTriggerValue(currentValue);
            alertEventRepository.save(event);
        }
    }

    private void handleNormalState(AlertRule rule, Double currentValue, List<AlertEvent> activeEvents) {
        String key = rule.getId().toString();

        violationCounter.remove(key);

        if (!activeEvents.isEmpty()) {
            AlertEvent event = activeEvents.get(0);
            event.resolve();
            alertEventRepository.save(event);
            log.info(" Алерт разрешен: {}", rule.getName());
        }
    }

    @Transactional
    public AlertRuleDto createRule(AlertRuleDto ruleDto) {
        AlertRule rule = ruleDto.toEntity();

        if (alertRuleRepository.existsByName(rule.getName())) {
            throw new IllegalArgumentException("Правило с именем '" + rule.getName() + "' уже существует");
        }

        AlertRule savedRule = alertRuleRepository.save(rule);
        return AlertRuleDto.fromEntity(savedRule);
    }

    @Transactional
    public AlertRuleDto updateRule(Long id, AlertRuleDto ruleDto) {
        AlertRule existingRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Правило не найдено с id: " + id));

        existingRule.setName(ruleDto.getName());
        existingRule.setMetricName(ruleDto.getMetricName());
        existingRule.setCondition(ruleDto.getCondition());
        existingRule.setThreshold(ruleDto.getThreshold());
        existingRule.setDurationSeconds(ruleDto.getDurationSeconds() != null ?
                ruleDto.getDurationSeconds() : 0);
        existingRule.setIsActive(ruleDto.getIsActive() != null ?
                ruleDto.getIsActive() : true);

        AlertRule updatedRule = alertRuleRepository.save(existingRule);
        return AlertRuleDto.fromEntity(updatedRule);
    }

    @Transactional
    public void deleteRule(Long id) {
        alertRuleRepository.deleteById(id);
    }

    @Transactional
    public AlertRuleDto toggleRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Правило не найдено с id: " + id));

        rule.setIsActive(!rule.getIsActive());
        AlertRule savedRule = alertRuleRepository.save(rule);
        return AlertRuleDto.fromEntity(savedRule);
    }

    public List<AlertRuleDto> getAllRules() {
        return alertRuleRepository.findAll().stream()
                .map(AlertRuleDto::fromEntity)
                .collect(Collectors.toList());
    }

    public AlertRuleDto getRuleById(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Правило не найдено с id: " + id));
        return AlertRuleDto.fromEntity(rule);
    }

    public List<AlertEventDto> getActiveAlerts() {
        return alertEventRepository.findAllByStatusOrderByStartedAtDesc("triggered")
                .stream()
                .map(AlertEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AlertEventDto> getAllAlertEvents() {
        return alertEventRepository.findLast50Events()
                .stream()
                .map(AlertEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    public long getActiveAlertsCount() {
        return alertEventRepository.countByStatus("triggered");
    }
}