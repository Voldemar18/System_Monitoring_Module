package ru.student.testing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.student.testing.entity.Metric;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricDto {
    private Long id;
    private LocalDateTime timestamp;
    private String host;
    private String metricName;
    private Double value;
    private String tags;

    public static MetricDto fromEntity(Metric metric) {
        return MetricDto.builder()
                .id(metric.getId())
                .timestamp(metric.getTimestamp())
                .host(metric.getHost())
                .metricName(metric.getMetricName())
                .value(metric.getValue())
                .tags(metric.getTags())
                .build();
    }

    public Metric toEntity() {
        return Metric.builder()
                .id(this.id)
                .timestamp(this.timestamp)
                .host(this.host)
                .metricName(this.metricName)
                .value(this.value)
                .tags(this.tags)
                .build();
    }
}