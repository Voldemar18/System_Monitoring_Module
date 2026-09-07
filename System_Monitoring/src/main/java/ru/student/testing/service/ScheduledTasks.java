package ru.student.testing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final MetricService metricService;
    private final AlertService alertService;

    /**
     * Сбор метрик каждые 10 секунд
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void collectMetrics() {
        try {
            metricService.collectAndSaveMetrics();
        } catch (Exception e) {
            log.error("Ошибка при сборе метрик: {}", e.getMessage());
        }
    }

    /**
     * Проверка алертов каждые 5 секунд (после сбора метрик)
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 7000)
    public void checkAlerts() {
        try {
            // Получаем последние метрики и проверяем алерты
            Map<String, Double> latestMetrics = metricService.getLatestAllMetrics();
            alertService.checkAlerts(latestMetrics);
        } catch (Exception e) {
            log.error("Ошибка при проверке алертов: {}", e.getMessage());
        }
    }

    /**
     * Очистка старых метрик каждый день в 03:00
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldMetrics() {
        try {
            metricService.cleanOldMetrics(30); // Удаляем метрики старше 30 дней
            log.info("Очистка старых метрик выполнена");
        } catch (Exception e) {
            log.error("Ошибка при очистке метрик: {}", e.getMessage());
        }
    }
}