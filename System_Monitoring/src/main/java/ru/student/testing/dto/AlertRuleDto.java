package ru.student.testing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.student.testing.entity.AlertRule;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleDto {
    private Long id;

    @NotBlank(message = "Название правила обязательно")
    private String name;

    @NotBlank(message = "Имя метрики обязательно")
    private String metricName;

    @NotBlank(message = "Условие обязательно (>, <, >=, <=, ==, !=)")
    private String condition;

    @NotNull(message = "Пороговое значение обязательно")
    @Positive(message = "Порог должен быть положительным числом")
    private Double threshold;

    private Integer durationSeconds;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static AlertRuleDto fromEntity(AlertRule rule) {
        return AlertRuleDto.builder()
                .id(rule.getId())
                .name(rule.getName())
                .metricName(rule.getMetricName())
                .condition(rule.getCondition())
                .threshold(rule.getThreshold())
                .durationSeconds(rule.getDurationSeconds())
                .isActive(rule.getIsActive())
                .createdAt(rule.getCreatedAt())
                .build();
    }

    public AlertRule toEntity() {
        return AlertRule.builder()
                .id(this.id)
                .name(this.name)
                .metricName(this.metricName)
                .condition(this.condition)
                .threshold(this.threshold)
                .durationSeconds(this.durationSeconds != null ? this.durationSeconds : 0)
                .isActive(this.isActive != null ? this.isActive : true)
                .createdAt(this.createdAt != null ? this.createdAt : LocalDateTime.now())
                .build();
    }
}