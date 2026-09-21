package ru.student.testing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.student.testing.dto.MetricDto;
import ru.student.testing.entity.AlertEvent;
import ru.student.testing.entity.AlertRule;
import ru.student.testing.entity.Metric;
import ru.student.testing.repository.AlertEventRepository;
import ru.student.testing.repository.MetricRepository;
import ru.student.testing.util.TelegramNotifier;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    private final TelegramNotifier telegramNotifier;
    private final MetricRepository metricRepository;
    private final SystemMetricsCollector metricsCollector;
    private final AlertEventRepository alertEventRepository;

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

        Double cpu = latest.get("cpu_percent");
        if (cpu != null) {
            String status = getHealthStatus(cpu, 70, 90);
            report.append(String.format("CPU: %5.1f%%   Статус: %s\n", cpu, status));
        }

        Double load1 = latest.get("load_avg_1min");
        if (load1 != null) {
            report.append(String.format("Load Avg (1мин): %.2f\n", load1));
        }

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

    @Override
    @Transactional(readOnly = true)
    public String buildReportForLastMinutes(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("minutes должно быть > 0");
        }

        LocalDateTime from = LocalDateTime.now().minusMinutes(minutes);
        List<String> metricNames = List.of(
                "cpu_percent", "ram_used_percent", "disk_used_percent",
                "network_rx_mb", "network_tx_mb"
        );

        List<Metric> metrics = metricRepository.findMetricsSince(metricNames, from);

        if (metrics.isEmpty()) {
            return "<b>📊 Отчёт за последние " + minutes + " мин</b>\n\n"
                    + "Нет данных за этот период.";
        }

        DateTimeFormatter minuteFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        Map<String, Map<String, Double>> byMinute = new TreeMap<>();

        for (Metric m : metrics) {
            String minute = m.getTimestamp().format(minuteFmt);
            byMinute
                    .computeIfAbsent(minute, k -> new HashMap<>())
                    .put(m.getMetricName(), m.getValue());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<b>📊 Отчёт за последние ").append(minutes).append(" мин</b>\n\n");

        sb.append("<pre>");
        sb.append(String.format("%-8s %6s %6s %6s %8s %8s%n",
                "Время", "CPU%", "RAM%", "Disk%", "Net RX", "Net TX"));
        sb.append("──────────────────────────────────────────────────\n");

        for (Map.Entry<String, Map<String, Double>> entry : byMinute.entrySet()) {
            String minute = entry.getKey();
            Map<String, Double> row = entry.getValue();

            sb.append(String.format("%-8s %6s %6s %6s %8s %8s%n",
                    minute,
                    fmt(row.get("cpu_percent")),
                    fmt(row.get("ram_used_percent")),
                    fmt(row.get("disk_used_percent")),
                    fmt(row.get("network_rx_mb")),
                    fmt(row.get("network_tx_mb"))));
        }
        sb.append("</pre>\n");

        List<AlertEvent> activeAlerts =
                alertEventRepository.findAllByStatusOrderByStartedAtDesc("triggered");

        sb.append("\n🔔 Активных алертов: <b>").append(activeAlerts.size()).append("</b>\n");

        if (!activeAlerts.isEmpty()) {
            DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
            int i = 1;
            for (AlertEvent event : activeAlerts) {
                AlertRule rule = event.getRule();
                String ruleName = rule != null ? rule.getName() : "Без названия";
                String metricName = rule != null ? rule.getMetricName() : "?";
                Double value = event.getTriggerValue();
                String condition = rule != null ? rule.getCondition() : "?";
                Double threshold = rule != null ? rule.getThreshold() : null;
                String startedAt = event.getStartedAt() != null
                        ? event.getStartedAt().format(dtFmt) : "—";

                sb.append("\n<b>Алерт ").append(i++).append("</b>\n");
                sb.append("⚠️ ").append(escapeHtml(ruleName)).append("\n");
                sb.append("📊 ").append(escapeHtml(metricName))
                        .append(" = ")
                        .append(value != null ? String.format("%.1f", value) : "—")
                        .append("\n");
                sb.append("🎯 Порог: ").append(escapeHtml(condition)).append(" ")
                        .append(threshold != null ? threshold : "—").append("\n");
                sb.append("Дата и время: ").append(startedAt);
            }
        }

        return sb.toString();
    }

    /** Отправка отчёта в Telegram */
    @Override
    public boolean sendReportToTelegram(int minutes) {
        String report = buildReportForLastMinutes(minutes);
        return telegramNotifier.sendMessage(report, "HTML");
    }

    private String shortName(String metricName) {
        return switch (metricName) {
            case "cpu_percent" -> "CPU %";
            case "ram_used_percent" -> "RAM %";
            case "disk_used_percent" -> "Disk %";
            case "network_rx_mb" -> "Net RX";
            case "network_tx_mb" -> "Net TX";
            default -> metricName;
        };
    }

    private String fmt(Double v) {
        return v == null ? "—" : String.format("%.2f", v);
    }

    /** Экранирование HTML для parse_mode=HTML */
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}