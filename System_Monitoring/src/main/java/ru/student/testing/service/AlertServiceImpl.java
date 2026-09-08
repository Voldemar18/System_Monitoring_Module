package ru.student.testing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.student.testing.dto.AlertEventDto;
import ru.student.testing.dto.AlertRuleDto;
import ru.student.testing.entity.AlertEvent;
import ru.student.testing.entity.AlertRule;
import ru.student.testing.repository.AlertEventRepository;
import ru.student.testing.repository.AlertRuleRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl extends BaseService<AlertRule> implements IAlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertEventRepository alertEventRepository;
    private final IMetricService metricService;

    private final Map<String, Double> lastMetricValues = new HashMap<>();
    private final Map<String, Integer> violationCounter = new HashMap<>();

    @Override
    public String getEntityType() {
        return "AlertRule";
    }

    @Override
    @Transactional
    public void checkAlerts(Map<String, Double> currentMetrics) {
        log.debug("Проверка алертов, метрик: {}", currentMetrics.size());

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
            event.initAuditFields();
            AlertEvent saved = alertEventRepository.save(event);
            log.info("Алерт СОЗДАН! ID: {}, Правило: {}, Значение: {}",
                    saved.getId(), rule.getName(), currentValue);
        } else {
            AlertEvent event = activeEvents.get(0);
            event.setTriggerValue(currentValue);
            event.initAuditFields();
            alertEventRepository.save(event);
            log.debug("Алерт обновлен: {}", rule.getName());
        }
    }

    private void handleNormalState(AlertRule rule, Double currentValue, List<AlertEvent> activeEvents) {
        String key = rule.getId().toString();
        violationCounter.remove(key);

        if (!activeEvents.isEmpty()) {
            AlertEvent event = activeEvents.get(0);
            event.resolve();
            alertEventRepository.save(event);
            log.info("Алерт РАЗРЕШЕН: {}", rule.getName());
        }
    }

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

    @Override
    @Transactional
    public void deleteRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Правило не найдено с id: " + id));
        alertRuleRepository.deleteById(id);
        logDeletion(rule);
    }

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

    @Override
    public List<AlertRuleDto> getAllRules() {
        return alertRuleRepository.findAll().stream()
                .map(AlertRuleDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public AlertRuleDto getRuleById(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Правило не найдено с id: " + id));
        return AlertRuleDto.fromEntity(rule);
    }

    @Override
    public List<AlertEventDto> getActiveAlerts() {
        return alertEventRepository.findAllByStatusOrderByStartedAtDesc("triggered")
                .stream()
                .map(AlertEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertEventDto> getAllAlertEvents() {
        log.info("📊 Вызов getAllAlertEvents()");
        try {
            long totalCount = alertEventRepository.count();
            log.info(" Всего записей в alert_events: {}", totalCount);

            List<AlertEvent> events = alertEventRepository.findLast50EventsNative();
            log.info("Native query вернула {} событий", events != null ? events.size() : 0);

            if (events == null) {
                events = new ArrayList<>();
            }

            if (!events.isEmpty()) {
                AlertEvent first = events.get(0);
                log.info("Первое событие: ID={}, статус={}, правило={}",
                        first.getId(),
                        first.getStatus(),
                        first.getRule() != null ? first.getRule().getName() : "null");
            }

            return events.stream()
                    .map(AlertEventDto::fromEntity)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Ошибка при получении событий алертов: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<AlertEventDto> getRecentAlertEvents(int limit) {
        log.info("Вызов getRecentAlertEvents({})", limit);
        try {
            List<AlertEvent> events = alertEventRepository.findLastNEvents(limit);
            log.info("Получено {} событий", events.size());
            return events.stream()
                    .map(AlertEventDto::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении последних {} событий: {}", limit, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<AlertEventDto> getAlertEventsForLastHours(int hours) {
        log.info("Вызов getAlertEventsForLastHours({})", hours);
        try {
            LocalDateTime from = LocalDateTime.now().minusHours(hours);
            List<AlertEvent> events = alertEventRepository.findByStartedAtAfterOrderByStartedAtDesc(from);
            log.info("Получено {} событий за последние {} часов", events.size(), hours);
            return events.stream()
                    .map(AlertEventDto::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении событий за последние {} часов: {}", hours, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<AlertEventDto> getAlertEventsForLastHours(int hours, int page, int size) {
        log.info("Вызов getAlertEventsForLastHours({}, {}, {})", hours, page, size);
        try {
            LocalDateTime from = LocalDateTime.now().minusHours(hours);
            Pageable pageable = PageRequest.of(page, size);
            Page<AlertEvent> eventsPage = alertEventRepository.findByStartedAtAfter(from, pageable);
            log.info("Получено {} событий (страница {}, размер {})",
                    eventsPage.getContent().size(), page, size);
            return eventsPage.getContent().stream()
                    .map(AlertEventDto::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении событий с пагинацией: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public long getActiveAlertsCount() {
        return alertEventRepository.countByStatus("triggered");
    }
}