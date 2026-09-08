package ru.student.testing.service;

import ru.student.testing.dto.AlertEventDto;
import ru.student.testing.dto.AlertRuleDto;

import java.util.List;
import java.util.Map;

/**
 * Интерфейс сервиса для управления алертами.
 * Определяет контракт для всех реализаций сервиса алертов.
 */
public interface IAlertService {

    void checkAlerts(Map<String, Double> currentMetrics);

    AlertRuleDto createRule(AlertRuleDto ruleDto);

    AlertRuleDto updateRule(Long id, AlertRuleDto ruleDto);

    void deleteRule(Long id);

    AlertRuleDto toggleRule(Long id);

    List<AlertRuleDto> getAllRules();

    AlertRuleDto getRuleById(Long id);

    List<AlertEventDto> getActiveAlerts();

    List<AlertEventDto> getAllAlertEvents();

    List<AlertEventDto> getRecentAlertEvents(int limit);

    // ===== НОВЫЕ МЕТОДЫ ДЛЯ ФИЛЬТРАЦИИ =====
    List<AlertEventDto> getAlertEventsForLastHours(int hours);

    List<AlertEventDto> getAlertEventsForLastHours(int hours, int page, int size);

    long getActiveAlertsCount();
}