package ru.student.testing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.student.testing.dto.AlertEventDto;
import ru.student.testing.dto.AlertRuleDto;
import ru.student.testing.service.AlertService;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/rules")
    public ResponseEntity<List<AlertRuleDto>> getAllRules() {
        return ResponseEntity.ok(alertService.getAllRules());
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<AlertRuleDto> getRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getRuleById(id));
    }

    @PostMapping("/rules")
    public ResponseEntity<AlertRuleDto> createRule(@Valid @RequestBody AlertRuleDto ruleDto) {
        AlertRuleDto created = alertService.createRule(ruleDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<AlertRuleDto> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody AlertRuleDto ruleDto) {
        return ResponseEntity.ok(alertService.updateRule(id, ruleDto));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        alertService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rules/{id}/toggle")
    public ResponseEntity<AlertRuleDto> toggleRule(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.toggleRule(id));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AlertEventDto>> getActiveAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    @GetMapping("/events")
    public ResponseEntity<List<AlertEventDto>> getAllEvents() {
        return ResponseEntity.ok(alertService.getAllAlertEvents());
    }
    @GetMapping("/active/count")
    public ResponseEntity<Long> getActiveAlertsCount() {
        return ResponseEntity.ok(alertService.getActiveAlertsCount());
    }
}