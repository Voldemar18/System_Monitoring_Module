package ru.student.testing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.student.testing.entity.Metric;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    List<Metric> findTop100ByMetricNameOrderByTimestampDesc(String metricName);

    List<Metric> findByMetricNameAndTimestampBetweenOrderByTimestampAsc(
            String metricName,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query(value = """
            SELECT DISTINCT ON (metric_name)
                metric_name, value, timestamp
            FROM metrics
            ORDER BY metric_name, timestamp DESC
            """, nativeQuery = true)
    List<Object[]> findLatestEachMetric();

    @Query(value = """
            SELECT value
            FROM metrics
            WHERE metric_name = :metricName
            ORDER BY timestamp DESC
            LIMIT 1
            """, nativeQuery = true)
    Double findLatestValueByMetricName(@Param("metricName") String metricName);

    @Query(value = """
            SELECT AVG(value)
            FROM metrics
            WHERE metric_name = :metricName
                AND timestamp >= NOW() - INTERVAL '1 hour'
            """, nativeQuery = true)
    Double findAverageLastHour(@Param("metricName") String metricName);

    @Query(value = """
            DELETE FROM metrics
            WHERE timestamp < NOW() - INTERVAL :days days
            """, nativeQuery = true)
    void deleteOlderThan(@Param("days") int days);

    @Query("SELECT DISTINCT m.metricName FROM Metric m")
    List<String> findAllMetricNames();
}