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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для управления алертами.
 * Наследует BaseService для использования общих методов логирования
 * и реализует интерфейс IAlertService для обеспечения контракта.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl extends BaseService<AlertRule> implements IAlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertEventRepository alertEventRepository;
    private final IMetricService metricService;

    /**
     * Хранит последние значения метрик для отслеживания изменений
     */
    private final Map<String, Double> lastMetricValues = new HashMap<>();

    /**
     * Счетчик нарушений для каждого правила (используется для duration)
     */
    private final Map<String, Integer> violationCounter = new HashMap<>();

    @Override
    public String getEntityType() {
        return "AlertRule";
    }

    /**
     * Проверяет все активные правила на соответствие текущим метрикам.
     * Для каждого правила проверяется текущее значение метрики и,
     * если условие нарушено, создается или обновляется событие алерта.
     */
    @Override
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

    /**
     * Обрабатывает ситуацию, когда условие правила нарушено.
     * Учитывает durationSeconds - если задано, то ждет накопления нарушений.
     */
    private void handleViolation(AlertRule rule, Double currentValue, List<AlertEvent> activeEvents) {
        String key = rule.getId().toString();

        // Если задана длительность, считаем количество нарушений
        if (rule.getDurationSeconds() > 0) {
            violationCounter.merge(key, 1, Integer::sum);
            int requiredCount = rule.getDurationSeconds() / 5;

            if (violationCounter.get(key) < requiredCount) {
                return;
            }
        }

        // Если нет активных событий - создаем новое
        if (activeEvents.isEmpty()) {
            AlertEvent event = new AlertEvent(rule, currentValue);
            event.initAuditFields();
            alertEventRepository.save(event);
            log.warn("Алерт сработал! Правило: {}, Значение: {}",
                    rule.getName(), currentValue);
        } else {
            AlertEvent event = activeEvents.get(0);
            event.setTriggerValue(currentValue);
            event.initAuditFields();
            alertEventRepository.save(event);
        }
    }

    /**
     * Обрабатывает нормальное состояние (условие не нарушено).
     * Сбрасывает счетчик нарушений и разрешает активные алерты.
     */
    private void handleNormalState(AlertRule rule, Double currentValue, List<AlertEvent> activeEvents) {
        String key = rule.getId().toString();

        violationCounter.remove(key);

        if (!activeEvents.isEmpty()) {
            AlertEvent event = activeEvents.get(0);
            event.resolve();
            alertEventRepository.save(event);
            log.info("Алерт разрешен: {}", rule.getName());
        }
    }

    /**
     * Создает новое правило алерта.
     * Проверяет, что правило с таким именем не существует.
     */
    @Override
    @Transactional
    public AlertRuleDto createRule(AlertRuleDto ruleDto) {
        AlertRule rule = ruleDto.toEntity();
        rule.initAuditFields();

        if (alertRuleRepository.existsByName(rule.getName())) {
            throw new IllegalArgumentException("Правило с именем '" + rule.getName() + "' уже существует");
        }

        AlertRule savedRule = alertRuleRepository.save(rule);
        logCreation(savedRule);
        return AlertRuleDto.fromEntity(savedRule);
    }

    //Обновляет существующее правило алерта.
    @Override
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
        existingRule.initAuditFields();

        AlertRule updatedRule = alertRuleRepository.save(existingRule);
        logUpdate(updatedRule);
        return AlertRuleDto.fromEntity(updatedRule);
    }

    //Удаляет правило алерта по идентификатору.
    @Override
    @Transactional
    public void deleteRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Правило не найдено с id: " + id));
        alertRuleRepository.deleteById(id);
        logDeletion(rule);
    }

    //Переключает состояние правила (активно/неактивно).
    @Override
    @Transactional
    public AlertRuleDto toggleRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Правило не найдено с id: " + id));

        rule.setIsActive(!rule.getIsActive());
        rule.initAuditFields();
        AlertRule savedRule = alertRuleRepository.save(rule);
        logUpdate(savedRule);
        return AlertRuleDto.fromEntity(savedRule);
    }

    //Получает все правила алертов.
    @Override
    public List<AlertRuleDto> getAllRules() {
        return alertRuleRepository.findAll().stream()
                .map(AlertRuleDto::fromEntity)
                .collect(Collectors.toList());
    }

    //Получает правило по идентификатору.

    @Override
    public AlertRuleDto getRuleById(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Правило не найдено с id: " + id));
        return AlertRuleDto.fromEntity(rule);
    }

    //Получает все активные (неразрешенные) алерты.

    @Override
    public List<AlertEventDto> getActiveAlerts() {
        return alertEventRepository.findAllByStatusOrderByStartedAtDesc("triggered")
                .stream()
                .map(AlertEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    //Получает последние 50 событий алертов.

    @Override
    public List<AlertEventDto> getAllAlertEvents() {
        return alertEventRepository.findLast50Events()
                .stream()
                .map(AlertEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    //Получает количество активных (неразрешенных) алертов.
    @Override
    public long getActiveAlertsCount() {
        return alertEventRepository.countByStatus("triggered");
    }
}