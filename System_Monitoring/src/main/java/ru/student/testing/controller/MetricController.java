package ru.student.testing.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.student.testing.dto.MetricDto;
import ru.student.testing.service.IMetricService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST контроллер для работы с метриками системы.
 * Предоставляет API для получения текущих значений, истории и отчетов.
 */
@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final IMetricService metricService;

    /**
     * Получить последние значения всех метрик.
     */
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Double>> getLatestAll() {
        log.debug("GET /api/metrics/latest - получение всех метрик");
        return ResponseEntity.ok(metricService.getLatestAllMetrics());
    }

    /**
     * Получить последнее значение конкретной метрики.
     */
    @GetMapping("/latest/{metricName}")
    public ResponseEntity<Double> getLatest(@PathVariable String metricName) {
        log.debug("GET /api/metrics/latest/{} - получение последнего значения", metricName);
        Double value = metricService.getLatestValue(metricName);
        return value != null ? ResponseEntity.ok(value) : ResponseEntity.notFound().build();
    }

    /**
     * Получить историю метрики за указанный период.
     */
    @GetMapping("/history")
    public ResponseEntity<List<MetricDto>> getHistory(
            @RequestParam String metricName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        log.debug("GET /api/metrics/history - метрика: {}, с: {}, по: {}", metricName, from, to);
        List<MetricDto> history = metricService.getMetricHistory(metricName, from, to);
        return ResponseEntity.ok(history);
    }

    /**
     * Получить последние N записей для указанной метрики.
     */
    @GetMapping("/latest/{metricName}/{limit}")
    public ResponseEntity<List<MetricDto>> getLatest(
            @PathVariable String metricName,
            @PathVariable int limit) {

        log.debug("GET /api/metrics/latest/{}/{} - получение последних записей", metricName, limit);
        List<MetricDto> metrics = metricService.getLatestMetrics(metricName, limit);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Получить список всех доступных названий метрик.
     */
    @GetMapping("/names")
    public ResponseEntity<List<String>> getMetricNames() {
        log.debug("GET /api/metrics/names - получение списка метрик");
        return ResponseEntity.ok(metricService.getAllMetricNames());
    }

    /**
     * Получить среднее значение метрики за последний час.
     */
    @GetMapping("/avg/{metricName}")
    public ResponseEntity<Double> getAverageLastHour(@PathVariable String metricName) {
        log.debug("GET /api/metrics/avg/{} - получение среднего за час", metricName);
        Double avg = metricService.getAverageLastHour(metricName);
        return avg != null ? ResponseEntity.ok(avg) : ResponseEntity.notFound().build();
    }

    /**
     * Принудительный сбор и сохранение метрик.
     */
    @PostMapping("/collect")
    public ResponseEntity<String> collectNow() {
        log.info("POST /api/metrics/collect - принудительный сбор метрик");
        metricService.collectAndSaveMetrics();
        return ResponseEntity.ok("Метрики успешно собраны");
    }

    /**
     * Получить отчет о состоянии системы.
     */
    @GetMapping("/report")
    public ResponseEntity<String> getSystemReport() {
        log.debug("GET /api/metrics/report - генерация отчета");
        String report = metricService.generateSystemReport();
        return ResponseEntity.ok(report);
    }
}