package ru.student.testing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.student.testing.entity.AlertEvent;
import ru.student.testing.entity.AlertRule;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEventDto {
    private Long id;
    private Long ruleId;
    private String ruleName;
    private String metricName;
    private Double threshold;
    private Double triggerValue;
    private LocalDateTime startedAt;
    private LocalDateTime resolvedAt;
    private String status;

    public static AlertEventDto fromEntity(AlertEvent event) {
        AlertRule rule = event.getRule();
        return AlertEventDto.builder()
                .id(event.getId())
                .ruleId(rule != null ? rule.getId() : null)
                .ruleName(rule != null ? rule.getName() : null)
                .metricName(rule != null ? rule.getMetricName() : null)
                .threshold(rule != null ? rule.getThreshold() : null)
                .triggerValue(event.getTriggerValue())
                .startedAt(event.getStartedAt())
                .resolvedAt(event.getResolvedAt())
                .status(event.getStatus())
                .build();
    }
}