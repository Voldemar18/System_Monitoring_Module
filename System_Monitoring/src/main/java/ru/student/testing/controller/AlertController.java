package ru.student.testing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.student.testing.dto.AlertEventDto;
import ru.student.testing.dto.AlertRuleDto;
import ru.student.testing.service.IAlertService;

import java.util.List;

/**
 * REST контроллер для управления алертами и правилами алертов.
 * Предоставляет API для CRUD операций с правилами и просмотра событий алертов.
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final IAlertService alertService;

    /**
     * Получить все правила алертов.
     */
    @GetMapping("/rules")
    public ResponseEntity<List<AlertRuleDto>> getAllRules() {
        log.debug("GET /api/alerts/rules - получение всех правил");
        return ResponseEntity.ok(alertService.getAllRules());
    }

    /**
     * Получить правило по идентификатору.
     */
    @GetMapping("/rules/{id}")
    public ResponseEntity<AlertRuleDto> getRuleById(@PathVariable Long id) {
        log.debug("GET /api/alerts/rules/{} - получение правила", id);
        return ResponseEntity.ok(alertService.getRuleById(id));
    }

    /**
     * Создать новое правило алерта.
     */
    @PostMapping("/rules")
    public ResponseEntity<AlertRuleDto> createRule(@Valid @RequestBody AlertRuleDto ruleDto) {
        log.info("POST /api/alerts/rules - создание правила: {}", ruleDto.getName());
        AlertRuleDto created = alertService.createRule(ruleDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Обновить существующее правило.
     */
    @PutMapping("/rules/{id}")
    public ResponseEntity<AlertRuleDto> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody AlertRuleDto ruleDto) {
        log.info("PUT /api/alerts/rules/{} - обновление правила", id);
        return ResponseEntity.ok(alertService.updateRule(id, ruleDto));
    }

    /**
     * Удалить правило.
     */
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        log.info("DELETE /api/alerts/rules/{} - удаление правила", id);
        alertService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Переключить состояние правила (активно/неактивно).
     */
    @PatchMapping("/rules/{id}/toggle")
    public ResponseEntity<AlertRuleDto> toggleRule(@PathVariable Long id) {
        log.info("PATCH /api/alerts/rules/{}/toggle - переключение состояния правила", id);
        return ResponseEntity.ok(alertService.toggleRule(id));
    }

    /**
     * Получить все активные (неразрешенные) алерты.
     */
    @GetMapping("/active")
    public ResponseEntity<List<AlertEventDto>> getActiveAlerts() {
        log.debug("GET /api/alerts/active - получение активных алертов");
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    /**
     * Получить все события алертов (последние 50).
     */
    @GetMapping("/events")
    public ResponseEntity<List<AlertEventDto>> getAllEvents() {
        log.debug("GET /api/alerts/events - получение всех событий");
        return ResponseEntity.ok(alertService.getAllAlertEvents());
    }

    /**
     * Получить количество активных алертов.
     */
    @GetMapping("/active/count")
    public ResponseEntity<Long> getActiveAlertsCount() {
        log.debug("GET /api/alerts/active/count - получение количества активных алертов");
        return ResponseEntity.ok(alertService.getActiveAlertsCount());
    }
}