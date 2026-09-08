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

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final IAlertService alertService;

    @GetMapping("/rules")
    public ResponseEntity<List<AlertRuleDto>> getAllRules() {
        log.debug("GET /api/alerts/rules - получение всех правил");
        return ResponseEntity.ok(alertService.getAllRules());
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<AlertRuleDto> getRuleById(@PathVariable Long id) {
        log.debug("GET /api/alerts/rules/{} - получение правила", id);
        return ResponseEntity.ok(alertService.getRuleById(id));
    }

    @PostMapping("/rules")
    public ResponseEntity<AlertRuleDto> createRule(@Valid @RequestBody AlertRuleDto ruleDto) {
        log.info("POST /api/alerts/rules - создание правила: {}", ruleDto.getName());
        AlertRuleDto created = alertService.createRule(ruleDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<AlertRuleDto> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody AlertRuleDto ruleDto) {
        log.info("PUT /api/alerts/rules/{} - обновление правила", id);
        return ResponseEntity.ok(alertService.updateRule(id, ruleDto));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        log.info("DELETE /api/alerts/rules/{} - удаление правила", id);
        alertService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rules/{id}/toggle")
    public ResponseEntity<AlertRuleDto> toggleRule(@PathVariable Long id) {
        log.info("PATCH /api/alerts/rules/{}/toggle - переключение состояния правила", id);
        return ResponseEntity.ok(alertService.toggleRule(id));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AlertEventDto>> getActiveAlerts() {
        log.debug("GET /api/alerts/active - получение активных алертов");
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    @GetMapping("/events")
    public ResponseEntity<List<AlertEventDto>> getAllEvents(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer hours,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.debug("GET /api/alerts/events - получение событий с параметрами: limit={}, hours={}, page={}, size={}",
                limit, hours, page, size);

        List<AlertEventDto> events;

        if (hours != null && hours > 0) {
            if (limit != null && limit > 0) {
                events = alertService.getAlertEventsForLastHours(hours, page, limit);
            } else {
                events = alertService.getAlertEventsForLastHours(hours);
            }
        } else if (limit != null && limit > 0) {
            events = alertService.getRecentAlertEvents(limit);
        } else {
            events = alertService.getRecentAlertEvents(50);
        }

        return ResponseEntity.ok(events);
    }

    @GetMapping("/active/count")
    public ResponseEntity<Long> getActiveAlertsCount() {
        log.debug("GET /api/alerts/active/count - получение количества активных алертов");
        return ResponseEntity.ok(alertService.getActiveAlertsCount());
    }
}