package ru.student.testing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.student.testing.entity.AlertRule;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findAllByIsActiveTrue();
    List<AlertRule> findAllByIsActiveTrueAndMetricName(String metricName);
    boolean existsByName(String name);
}