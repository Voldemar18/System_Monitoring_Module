package ru.student.testing.util;

import org.springframework.stereotype.Component;
import ru.student.testing.dto.MetricDto;
import ru.student.testing.entity.Metric;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MetricConverter {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MetricDto toDto(Metric entity) {
        if (entity == null) {
            return null;
        }

        return MetricDto.builder()
                .id(entity.getId())
                .timestamp(entity.getTimestamp())
                .host(entity.getHost())
                .metricName(entity.getMetricName())
                .value(entity.getValue())
                .tags(entity.getTags())
                .build();
    }

    public Metric toEntity(MetricDto dto) {
        if (dto == null) {
            return null;
        }

        return Metric.builder()
                .id(dto.getId())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now())
                .host(dto.getHost() != null ? dto.getHost() : "localhost")
                .metricName(dto.getMetricName())
                .value(dto.getValue())
                .tags(dto.getTags() != null ? dto.getTags() : "{}")
                .build();
    }

    public List<MetricDto> toDtoList(List<Metric> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<Metric> toEntityList(List<MetricDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public Metric createMetric(String metricName, Double value) {
        return createMetric(metricName, value, "localhost", "{}");
    }

    public Metric createMetric(String metricName, Double value, String host) {
        return createMetric(metricName, value, host, "{}");
    }

    public Metric createMetric(String metricName, Double value, String host, String tags) {
        return Metric.builder()
                .timestamp(LocalDateTime.now())
                .host(host != null ? host : "localhost")
                .metricName(metricName)
                .value(value)
                .tags(tags != null ? tags : "{}")
                .build();
    }

    public String formatMetric(Metric metric) {
        if (metric == null) {
            return "null";
        }
        return String.format("[%s] %s: %.2f %s",
                metric.getTimestamp().format(DATE_FORMATTER),
                metric.getMetricName(),
                metric.getValue(),
                metric.getHost()
        );
    }

    public String formatValueWithUnit(Metric metric) {
        if (metric == null) {
            return "N/A";
        }
        return formatValueWithUnit(metric.getMetricName(), metric.getValue());
    }

    public String formatValueWithUnit(String metricName, Double value) {
        if (value == null) {
            return "N/A";
        }

        return switch (metricName) {
            case "cpu_percent", "ram_used_percent", "disk_used_percent" ->
                    String.format("%.1f%%", value);
            case "ram_total_mb", "ram_used_mb", "ram_available_mb" ->
                    String.format("%.0f MB", value);
            case "disk_total_gb", "disk_used_gb" ->
                    String.format("%.1f GB", value);
            case "load_avg_1min", "load_avg_5min", "load_avg_15min" ->
                    String.format("%.2f", value);
            case "network_rx_mb", "network_tx_mb" ->
                    String.format("%.2f MB/s", value);
            default -> String.format("%.2f", value);
        };
    }

    public String getMetricColor(String metricName, Double value) {
        if (value == null) {
            return "#95a5a6";
        }

        return switch (metricName) {
            case "cpu_percent" -> getColorByThreshold(value, 70, 90);
            case "ram_used_percent" -> getColorByThreshold(value, 75, 90);
            case "disk_used_percent" -> getColorByThreshold(value, 80, 95);
            default -> "#3498db";
        };
    }

    private String getColorByThreshold(double value, double warningThreshold, double criticalThreshold) {
        if (value > criticalThreshold) {
            return "#e74c3c";
        } else if (value > warningThreshold) {
            return "#f39c12";
        } else {
            return "#2ecc71";
        }
    }

    public String getMetricIcon(String metricName) {
        return switch (metricName) {
            case "cpu_percent", "load_avg_1min", "load_avg_5min", "load_avg_15min" ->
                    "";
            case "ram_total_mb", "ram_used_mb", "ram_available_mb", "ram_used_percent" ->
                    "";
            case "disk_total_gb", "disk_used_gb", "disk_used_percent" ->
                    "";
            case "network_rx_mb", "network_tx_mb" ->
                    "";
            default -> "";
        };
    }

    public String getHumanReadableName(String metricName) {
        return switch (metricName) {
            case "cpu_percent" -> "Загрузка CPU";
            case "load_avg_1min" -> "Средняя нагрузка (1 мин)";
            case "load_avg_5min" -> "Средняя нагрузка (5 мин)";
            case "load_avg_15min" -> "Средняя нагрузка (15 мин)";
            case "ram_total_mb" -> "Общая RAM";
            case "ram_used_mb" -> "Использованная RAM";
            case "ram_available_mb" -> "Доступная RAM";
            case "ram_used_percent" -> "Использование RAM";
            case "disk_total_gb" -> "Общий диск";
            case "disk_used_gb" -> "Использованный диск";
            case "disk_used_percent" -> "Использование диска";
            case "network_rx_mb" -> "Входящий трафик";
            case "network_tx_mb" -> "Исходящий трафик";
            default -> metricName.replace("_", " ").toUpperCase();
        };
    }

    public boolean isCriticalMetric(String metricName) {
        return List.of(
                "cpu_percent",
                "ram_used_percent",
                "disk_used_percent"
        ).contains(metricName);
    }

    public Double getRecommendedThreshold(String metricName) {
        return switch (metricName) {
            case "cpu_percent" -> 80.0;
            case "ram_used_percent" -> 85.0;
            case "disk_used_percent" -> 90.0;
            default -> 50.0;
        };
    }
}