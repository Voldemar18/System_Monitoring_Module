package ru.student.testing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.student.testing.dto.MetricDto;
import ru.student.testing.entity.Metric;
import ru.student.testing.repository.MetricRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricService {

    private final MetricRepository metricRepository;
    private final SystemMetricsCollector metricsCollector;

    /**
     * Сохранить метрики из Map в БД
     */
    @Transactional
    public void saveMetrics(Map<String, Double> metrics) {
        String host = "localhost"; // Можно получать из конфигурации

        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            Metric metric = new Metric(host, entry.getKey(), entry.getValue());
            metricRepository.save(metric);
        }

        log.debug("Сохранено {} метрик", metrics.size());
    }

    /**
     * Сохранить одну метрику
     */
    @Transactional
    public Metric saveMetric(String metricName, Double value) {
        Metric metric = new Metric("localhost", metricName, value);
        return metricRepository.save(metric);
    }

    /**
     * Получить последние 100 записей для метрики
     */
    public List<MetricDto> getLatestMetrics(String metricName, int limit) {
        List<Metric> metrics = metricRepository.findTop100ByMetricNameOrderByTimestampDesc(metricName);
        return metrics.stream()
                .limit(limit)
                .map(MetricDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Получить историю метрики за период
     */
    public List<MetricDto> getMetricHistory(String metricName, LocalDateTime from, LocalDateTime to) {
        List<Metric> metrics = metricRepository.findByMetricNameAndTimestampBetweenOrderByTimestampAsc(
                metricName, from, to
        );
        return metrics.stream()
                .map(MetricDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Получить последнее значение каждой метрики
     */
    public Map<String, Double> getLatestAllMetrics() {
        Map<String, Double> result = new HashMap<>();
        List<Object[]> latest = metricRepository.findLatestEachMetric();

        for (Object[] row : latest) {
            String metricName = (String) row[0];
            Double value = (Double) row[1];
            result.put(metricName, value);
        }

        return result;
    }

    /**
     * Получить последнее значение конкретной метрики
     */
    public Double getLatestValue(String metricName) {
        return metricRepository.findLatestValueByMetricName(metricName);
    }

    /**
     * Получить среднее значение за последний час
     */
    public Double getAverageLastHour(String metricName) {
        return metricRepository.findAverageLastHour(metricName);
    }

    /**
     * Получить все названия метрик
     */
    public List<String> getAllMetricNames() {
        return metricRepository.findAllMetricNames();
    }

    /**
     * Собрать и сохранить текущие метрики (вызывается по расписанию)
     */
    @Transactional
    public void collectAndSaveMetrics() {
        try {
            Map<String, Double> metrics = metricsCollector.collectAllMetrics();
            saveMetrics(metrics);
            log.info("Метрики успешно собраны и сохранены");
        } catch (Exception e) {
            log.error("Ошибка при сборе метрик: {}", e.getMessage(), e);
        }
    }

    /**
     * Получить базовые метрики для WebSocket
     */
    public Map<String, Double> getBasicMetrics() {
        return metricsCollector.collectBasicMetrics();
    }

    /**
     * Очистить старые метрики (вызывается по расписанию)
     */
    @Transactional
    public void cleanOldMetrics(int days) {
        metricRepository.deleteOlderThan(days);
        log.info("Удалены метрики старше {} дней", days);
    }
}