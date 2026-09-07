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

    // Храним последние значения метрик для проверки duration
    private final Map<String, Double> lastMetricValues = new HashMap<>();
    private final Map<String, Integer> violationCounter = new HashMap<>();

    /**
     * Проверить все активные правила на основе текущих метрик
     */
    @Transactional
    public void checkAlerts(Map<String, Double> currentMetrics) {
        List<AlertRule> activeRules = alertRuleRepository.findAllByIsActiveTrue();

        for (AlertRule rule : activeRules) {
            Double currentValue = currentMetrics.get(rule.getMetricName());
            if (currentValue == null) {
                continue;
            }

            // Проверяем, нарушено ли правило
            boolean isViolated = rule.isViolated(currentValue);

            // Проверяем, есть ли уже активный алерт для этого правила
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

    /**
     * Обработка нарушения правила
     */
    private void handleViolation(AlertRule rule, Double currentValue, List<AlertEvent> activeEvents) {
        String key = rule.getId().toString();

        // Если правило требует duration - считаем количество нарушений подряд
        if (rule.getDurationSeconds() > 0) {
            violationCounter.merge(key, 1, Integer::sum);
            int requiredCount = rule.getDurationSeconds() / 5; // 5 секунд - интервал сбора

            if (violationCounter.get(key) < requiredCount) {
                return; // Еще не накопили достаточно нарушений
            }
        }

        // Если нет активного алерта - создаем новый
        if (activeEvents.isEmpty()) {
            AlertEvent event = new AlertEvent(rule, currentValue);
            alertEventRepository.save(event);
            log.warn("⚠️ Алерт сработал! Правило: {}, Значение: {}",
                    rule.getName(), currentValue);
            // TODO: Отправить уведомление (email/telegram)
        }
        // Если активный алерт уже есть - обновляем значение
        else {
            AlertEvent event = activeEvents.get(0);
            event.setTriggerValue(currentValue);
            alertEventRepository.save(event);
        }
    }

    /**
     * Обработка нормального состояния (метрика в норме)
     */
    private void handleNormalState(AlertRule rule, Double currentValue, List<AlertEvent> activeEvents) {
        String key = rule.getId().toString();

        // Сбрасываем счетчик нарушений
        violationCounter.remove(key);

        // Если есть активный алерт - разрешаем его
        if (!activeEvents.isEmpty()) {
            AlertEvent event = activeEvents.get(0);
            event.resolve();
            alertEventRepository.save(event);
            log.info("✅ Алерт разрешен: {}", rule.getName());
        }
    }

    /**
     * Создать новое правило алерта
     */
    @Transactional
    public AlertRuleDto createRule(AlertRuleDto ruleDto) {
        AlertRule rule = ruleDto.toEntity();

        // Проверяем, что правило с таким именем не существует
        if (alertRuleRepository.existsByName(rule.getName())) {
            throw new IllegalArgumentException("Правило с именем '" + rule.getName() + "' уже существует");
        }

        AlertRule savedRule = alertRuleRepository.save(rule);
        return AlertRuleDto.fromEntity(savedRule);
    }

    /**
     * Обновить правило алерта
     */
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

    /**
     * Удалить правило алерта
     */
    @Transactional
    public void deleteRule(Long id) {
        alertRuleRepository.deleteById(id);
    }

    /**
     * Включить/выключить правило
     */
    @Transactional
    public AlertRuleDto toggleRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Правило не найдено с id: " + id));

        rule.setIsActive(!rule.getIsActive());
        AlertRule savedRule = alertRuleRepository.save(rule);
        return AlertRuleDto.fromEntity(savedRule);
    }

    /**
     * Получить все правила
     */
    public List<AlertRuleDto> getAllRules() {
        return alertRuleRepository.findAll().stream()
                .map(AlertRuleDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Получить правило по ID
     */
    public AlertRuleDto getRuleById(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Правило не найдено с id: " + id));
        return AlertRuleDto.fromEntity(rule);
    }

    /**
     * Получить все активные события алертов
     */
    public List<AlertEventDto> getActiveAlerts() {
        return alertEventRepository.findAllByStatusOrderByStartedAtDesc("triggered")
                .stream()
                .map(AlertEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Получить все события алертов
     */
    public List<AlertEventDto> getAllAlertEvents() {
        return alertEventRepository.findLast50Events()
                .stream()
                .map(AlertEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Получить количество активных алертов
     */
    public long getActiveAlertsCount() {
        return alertEventRepository.countByStatus("triggered");
    }
}