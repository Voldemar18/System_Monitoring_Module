package ru.student.testing.service;

import ru.student.testing.dto.MetricDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Интерфейс сервиса для работы с метриками системы.
 * Определяет контракт для всех реализаций сервиса метрик.
 */
public interface IMetricService {

    void saveMetrics(Map<String, Double> metrics);

    MetricDto saveMetric(String metricName, Double value);

    List<MetricDto> getLatestMetrics(String metricName, int limit);

    List<MetricDto> getMetricHistory(String metricName, LocalDateTime from, LocalDateTime to);

    Map<String, Double> getLatestAllMetrics();

    Double getLatestValue(String metricName);

    Double getAverageLastHour(String metricName);

    List<String> getAllMetricNames();

    void collectAndSaveMetrics();

    Map<String, Double> getBasicMetrics();

    void cleanOldMetrics(int days);

    String generateSystemReport();
}