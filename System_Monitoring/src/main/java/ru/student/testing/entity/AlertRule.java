package ru.student.testing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alert_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule extends BaseEntity {

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

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AlertEvent> alertEvents = new ArrayList<>();

    public AlertRule(String name, String metricName, String condition, Double threshold) {
        this.name = name;
        this.metricName = metricName;
        this.condition = condition;
        this.threshold = threshold;
        this.isActive = true;
        this.durationSeconds = 0;
        initAuditFields();
    }

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
        AlertRule rule = new AlertRule(name, metricName, condition, threshold);
        rule.initAuditFields();
        return rule;
    }

    @Override
    public String getEntityDisplayName() {
        return String.format("AlertRule[%s: %s %s %.2f]", name, metricName, condition, threshold);
    }
}