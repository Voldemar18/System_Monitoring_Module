package ru.student.testing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.student.testing.entity.AlertEvent;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {

    List<AlertEvent> findAllByStatusOrderByStartedAtDesc(String status);
    List<AlertEvent> findAllByRuleIdAndStatus(Long ruleId, String status);
    List<AlertEvent> findByStartedAtAfterOrderByStartedAtDesc(LocalDateTime date);
    long countByStatus(String status);

    @Query(value = """
            SELECT * FROM alert_events 
            ORDER BY started_at DESC 
            LIMIT 50
            """, nativeQuery = true)
    List<AlertEvent> findLast50Events();
}