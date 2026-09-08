package ru.student.testing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Класс для выполнения запланированных задач.
 * Содержит задачи по сбору метрик, проверке алертов и очистке старых данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final IMetricService metricService;
    private final IAlertService alertService;

    /**
     * Сбор метрик каждые 10 секунд.
     * Использует fixedDelay для гарантии, что следующая итерация начнется
     * только после завершения предыдущей.
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void collectMetrics() {
        try {
            metricService.collectAndSaveMetrics();
            log.debug("Метрики успешно собраны");
        } catch (Exception e) {
            log.error("Ошибка при сборе метрик: {}", e.getMessage(), e);
        }
    }

    /**
     * Проверка алертов каждые 5 секунд (после сбора метрик).
     * Получает последние метрики и передает их в сервис алертов для проверки.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 7000)
    public void checkAlerts() {
        try {
            Map<String, Double> latestMetrics = metricService.getLatestAllMetrics();
            alertService.checkAlerts(latestMetrics);
            log.debug("Проверка алертов выполнена");
        } catch (Exception e) {
            log.error("Ошибка при проверке алертов: {}", e.getMessage(), e);
        }
    }

    /**
     * Очистка старых метрик каждый день в 03:00.
     * Удаляет метрики старше 30 дней для экономии места в базе данных.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldMetrics() {
        try {
            metricService.cleanOldMetrics(30); // Удаляем метрики старше 30 дней
            log.info("Очистка старых метрик выполнена успешно");
        } catch (Exception e) {
            log.error("Ошибка при очистке метрик: {}", e.getMessage(), e);
        }
    }

    /**
     * Генерация отчета о состоянии системы каждые 5 минут.
     * Выводит в лог информационный отчет о текущем состоянии системы.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 10000)
    public void generateSystemReport() {
        try {
            String report = metricService.generateSystemReport();
            log.info("\n{}", report);
        } catch (Exception e) {
            log.error("Ошибка при генерации отчета: {}", e.getMessage(), e);
        }
    }
}