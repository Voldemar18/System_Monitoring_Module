package ru.student.testing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.student.testing.dto.MetricDto;
import ru.student.testing.entity.Metric;
import ru.student.testing.repository.MetricRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для работы с метриками системы.
 * Наследует BaseService для использования общих методов логирования
 * и реализует интерфейс IMetricService для обеспечения контракта.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricServiceImpl extends BaseService<Metric> implements IMetricService {

    private final MetricRepository metricRepository;
    private final SystemMetricsCollector metricsCollector;

    @Override
    public String getEntityType() {
        return "Metric";
    }

    @Override
    @Transactional
    public void saveMetrics(Map<String, Double> metrics) {
        String host = "localhost";

        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            Metric metric = new Metric(host, entry.getKey(), entry.getValue());
            metric.initAuditFields();
            Metric saved = metricRepository.save(metric);
            logCreation(saved);
        }

        log.debug("Сохранено {} метрик", metrics.size());
    }

    @Override
    @Transactional
    public MetricDto saveMetric(String metricName, Double value) {
        Metric metric = new Metric("localhost", metricName, value);
        metric.initAuditFields();
        Metric saved = metricRepository.save(metric);
        logCreation(saved);
        return MetricDto.fromEntity(saved);
    }

    @Override
    public List<MetricDto> getLatestMetrics(String metricName, int limit) {
        List<Metric> metrics = metricRepository.findTop100ByMetricNameOrderByTimestampDesc(metricName);
        return metrics.stream()
                .limit(limit)
                .map(MetricDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<MetricDto> getMetricHistory(String metricName, LocalDateTime from, LocalDateTime to) {
        List<Metric> metrics = metricRepository.findByMetricNameAndTimestampBetweenOrderByTimestampAsc(
                metricName, from, to
        );
        return metrics.stream()
                .map(MetricDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
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

    @Override
    public Double getLatestValue(String metricName) {
        return metricRepository.findLatestValueByMetricName(metricName);
    }

    @Override
    public Double getAverageLastHour(String metricName) {
        return metricRepository.findAverageLastHour(metricName);
    }

    @Override
    public List<String> getAllMetricNames() {
        return metricRepository.findAllMetricNames();
    }

    @Override
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

    @Override
    public Map<String, Double> getBasicMetrics() {
        return metricsCollector.collectBasicMetrics();
    }

    @Override
    @Transactional
    public void cleanOldMetrics(int days) {
        metricRepository.deleteOlderThan(days);
        log.info("🗑️ Удалены метрики старше {} дней", days);
    }

    @Override
    public String generateSystemReport() {
        Map<String, Double> latest = getLatestAllMetrics();
        StringBuilder report = new StringBuilder();
        report.append("\n╔════════════════════════════════════════════════════════════╗\n");
        report.append("║           СИСТЕМНЫЙ ОТЧЕТ О СОСТОЯНИИ                 ║\n");
        report.append("╚════════════════════════════════════════════════════════════╝\n");
        report.append(" Время отчета: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // CPU
        Double cpu = latest.get("cpu_percent");
        if (cpu != null) {
            String status = getHealthStatus(cpu, 70, 90);
            report.append(String.format("CPU: %5.1f%%   Статус: %s\n", cpu, status));
        }

        // Load Average
        Double load1 = latest.get("load_avg_1min");
        if (load1 != null) {
            report.append(String.format("Load Avg (1мин): %.2f\n", load1));
        }

        // RAM
        Double ram = latest.get("ram_used_percent");
        if (ram != null) {
            String status = getHealthStatus(ram, 75, 90);
            Double total = latest.get("ram_total_mb");
            Double used = latest.get("ram_used_mb");
            report.append(String.format("RAM: %5.1f%%   Статус: %s", ram, status));
            if (total != null && used != null) {
                report.append(String.format(" (%.0f / %.0f MB)", used, total));
            }
            report.append("\n");
        }

        // Disk
        Double disk = latest.get("disk_used_percent");
        if (disk != null) {
            String status = getHealthStatus(disk, 80, 95);
            Double total = latest.get("disk_total_gb");
            Double used = latest.get("disk_used_gb");
            report.append(String.format("Disk: %5.1f%%   Статус: %s", disk, status));
            if (total != null && used != null) {
                report.append(String.format(" (%.1f / %.1f GB)", used, total));
            }
            report.append("\n");
        }

        // Network
        Double rx = latest.get("network_rx_mb");
        Double tx = latest.get("network_tx_mb");
        if (rx != null || tx != null) {
            report.append(String.format("Сеть: RX %.2f MB/s, TX %.2f MB/s\n",
                    rx != null ? rx : 0.0,
                    tx != null ? tx : 0.0));
        }

        long activeAlerts = 0;
        try {
            activeAlerts = metricRepository.count();
        } catch (Exception e) {
        }
        report.append("\nАктивных алертов: ").append(activeAlerts > 0 ? "⚠️ " : "✅ ").append(activeAlerts);

        report.append("\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("Отчет сформирован успешно\n");

        return report.toString();
    }
}