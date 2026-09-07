package ru.student.testing.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.student.testing.dto.MetricDto;
import ru.student.testing.service.MetricService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;

    /**
     * Получить последние значения всех метрик
     */
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Double>> getLatestAll() {
        return ResponseEntity.ok(metricService.getLatestAllMetrics());
    }

    /**
     * Получить последнее значение конкретной метрики
     */
    @GetMapping("/latest/{metricName}")
    public ResponseEntity<Double> getLatest(@PathVariable String metricName) {
        Double value = metricService.getLatestValue(metricName);
        return value != null ? ResponseEntity.ok(value) : ResponseEntity.notFound().build();
    }

    /**
     * Получить историю метрики за период
     */
    @GetMapping("/history")
    public ResponseEntity<List<MetricDto>> getHistory(
            @RequestParam String metricName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<MetricDto> history = metricService.getMetricHistory(metricName, from, to);
        return ResponseEntity.ok(history);
    }

    /**
     * Получить последние N записей для метрики
     */
    @GetMapping("/latest/{metricName}/{limit}")
    public ResponseEntity<List<MetricDto>> getLatest(
            @PathVariable String metricName,
            @PathVariable int limit) {

        List<MetricDto> metrics = metricService.getLatestMetrics(metricName, limit);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Получить все названия метрик
     */
    @GetMapping("/names")
    public ResponseEntity<List<String>> getMetricNames() {
        return ResponseEntity.ok(metricService.getAllMetricNames());
    }

    /**
     * Получить среднее значение за последний час
     */
    @GetMapping("/avg/{metricName}")
    public ResponseEntity<Double> getAverageLastHour(@PathVariable String metricName) {
        Double avg = metricService.getAverageLastHour(metricName);
        return avg != null ? ResponseEntity.ok(avg) : ResponseEntity.notFound().build();
    }

    /**
     * Принудительный сбор метрик (вручную)
     */
    @PostMapping("/collect")
    public ResponseEntity<String> collectNow() {
        metricService.collectAndSaveMetrics();
        return ResponseEntity.ok("Метрики успешно собраны");
    }
}