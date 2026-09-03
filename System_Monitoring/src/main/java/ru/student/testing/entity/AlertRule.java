package ru.student.testing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alert_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "metric_name", length = 50, nullable = false)
    private String metricName;

    @Column(name = "condition", length = 5, nullable = false)
    private String condition;

    @Column(name = "threshold", nullable = false)
    private Double threshold;

    @Column(name = "duration_seconds")
    @Builder.Default
    private Integer durationSeconds = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AlertEvent> alertEvents = new ArrayList<>();

    public boolean isViolated(Double currentValue) {
        if (currentValue == null) {
            return false;
        }
        return switch (condition) {
            case ">" -> currentValue > threshold;
            case "<" -> currentValue < threshold;
            case ">=" -> currentValue >= threshold;
            case "<=" -> currentValue <= threshold;
            case "==" -> currentValue.equals(threshold);
            case "!=" -> !currentValue.equals(threshold);
            default -> false;
        };
    }

    public static AlertRule createActive(String name, String metricName,
                                         String condition, Double threshold) {
        return AlertRule.builder()
                .name(name)
                .metricName(metricName)
                .condition(condition)
                .threshold(threshold)
                .isActive(true)
                .durationSeconds(0)
                .build();
    }
}