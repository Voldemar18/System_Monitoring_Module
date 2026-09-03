package ru.student.testing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "metrics",
        indexes = {
                @Index(name = "idx_metrics_timestamp", columnList = "timestamp"),
                @Index(name = "idx_metrics_name_time", columnList = "metricName, timestamp")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "host", length = 50)
    @Builder.Default
    private String host = "localhost";

    @Column(name = "metric_name", length = 50, nullable = false)
    private String metricName;

    @Column(name = "value", nullable = false)
    private Double value;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags;

    public Metric(String metricName, Double value) {
        this.timestamp = LocalDateTime.now();
        this.host = "localhost";
        this.metricName = metricName;
        this.value = value;
        this.tags = "{}";
    }

    public Metric(String host, String metricName, Double value) {
        this.timestamp = LocalDateTime.now();
        this.host = host;
        this.metricName = metricName;
        this.value = value;
        this.tags = "{}";
    }
}