package ru.student.testing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Метод для фильтрации по времени (без пагинации)
    List<AlertEvent> findByStartedAtAfterOrderByStartedAtDesc(LocalDateTime date);

    // Метод для фильтрации по времени с пагинацией
    Page<AlertEvent> findByStartedAtAfter(LocalDateTime date, Pageable pageable);

    long countByStatus(String status);

    // Native запрос для последних 50
    @Query(value = "SELECT * FROM alert_events ORDER BY started_at DESC LIMIT 50", nativeQuery = true)
    List<AlertEvent> findLast50EventsNative();

    // Native запрос с параметром limit
    @Query(value = "SELECT * FROM alert_events ORDER BY started_at DESC LIMIT :limit", nativeQuery = true)
    List<AlertEvent> findLastNEvents(@Param("limit") int limit);

    // Все с сортировкой
    List<AlertEvent> findAllByOrderByStartedAtDesc();
}