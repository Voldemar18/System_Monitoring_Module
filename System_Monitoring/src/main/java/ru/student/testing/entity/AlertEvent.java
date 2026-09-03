package ru.student.testing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_events",
        indexes = {
                @Index(name = "idx_alert_events_status", columnList = "status"),
                @Index(name = "idx_alert_events_started", columnList = "startedAt")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private AlertRule rule;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "triggered";

    @Column(name = "trigger_value")
    private Double triggerValue;

    public AlertEvent(AlertRule rule, Double triggerValue) {
        this.rule = rule;
        this.triggerValue = triggerValue;
        this.startedAt = LocalDateTime.now();
        this.status = "triggered";
    }

    public void resolve() {
        this.status = "resolved";
        this.resolvedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return "triggered".equals(status) && resolvedAt == null;
    }

    public boolean isResolved() {
        return "resolved".equals(status) && resolvedAt != null;
    }
}