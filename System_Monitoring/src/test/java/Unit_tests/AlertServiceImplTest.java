package Unit_tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.student.testing.dto.AlertEventDto;
import ru.student.testing.dto.AlertRuleDto;
import ru.student.testing.entity.AlertEvent;
import ru.student.testing.entity.AlertRule;
import ru.student.testing.repository.AlertEventRepository;
import ru.student.testing.repository.AlertRuleRepository;
import ru.student.testing.service.AlertServiceImpl;
import ru.student.testing.service.IMetricService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для AlertServiceImpl.
 * Проверяют создание правил, проверку алертов и управление событиями.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private AlertEventRepository alertEventRepository;

    @Mock
    private IMetricService metricService;

    @InjectMocks
    private AlertServiceImpl alertService;

    private AlertRule testRule;
    private AlertEvent testEvent;
    private Map<String, Double> testMetrics;

    @BeforeEach
    void setUp() {
        // Подготовка тестовых данных перед каждым тестом
        testRule = AlertRule.createActive("High CPU", "cpu_percent", ">", 80.0);
        testRule.setId(1L);
        testRule.initAuditFields();

        testEvent = new AlertEvent(testRule, 85.0);
        testEvent.setId(1L);
        testEvent.initAuditFields();

        testMetrics = new HashMap<>();
        testMetrics.put("cpu_percent", 85.0);
        testMetrics.put("ram_used_percent", 60.0);
        testMetrics.put("disk_used_percent", 75.0);
    }

    //ТЕСТ 1: Проверка типа сущности
    @Test
    void testGetEntityType() {
        // Проверяем, что сервис возвращает правильный тип сущности
        assertEquals("AlertRule", alertService.getEntityType());
    }

    //ТЕСТ 2: Создание правила
    @Test
    void testCreateRule() {
        // Проверяем создание нового правила алерта
        AlertRuleDto ruleDto = AlertRuleDto.builder()
                .name("High CPU")
                .metricName("cpu_percent")
                .condition(">")
                .threshold(80.0)
                .isActive(true)
                .build();

        when(alertRuleRepository.existsByName("High CPU")).thenReturn(false);
        when(alertRuleRepository.save(any(AlertRule.class))).thenReturn(testRule);

        AlertRuleDto result = alertService.createRule(ruleDto);

        assertNotNull(result);
        assertEquals("High CPU", result.getName());
        assertEquals("cpu_percent", result.getMetricName());
        assertEquals(80.0, result.getThreshold());
        verify(alertRuleRepository, times(1)).save(any(AlertRule.class));
    }

    //ТЕСТ 3: Создание правила с дублирующимся именем
    @Test
    void testCreateRuleDuplicateName() {
        // Проверяем, что при создании правила с существующим именем выбрасывается исключение
        AlertRuleDto ruleDto = AlertRuleDto.builder()
                .name("High CPU")
                .metricName("cpu_percent")
                .condition(">")
                .threshold(80.0)
                .build();

        when(alertRuleRepository.existsByName("High CPU")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> alertService.createRule(ruleDto));
        verify(alertRuleRepository, never()).save(any(AlertRule.class));
    }

    //ТЕСТ 4: Обновление правила
    @Test
    void testUpdateRule() {
        // Проверяем обновление существующего правила
        AlertRuleDto updateDto = AlertRuleDto.builder()
                .name("Critical CPU")
                .metricName("cpu_percent")
                .condition(">")
                .threshold(90.0)
                .isActive(true)
                .build();

        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenReturn(testRule);

        AlertRuleDto result = alertService.updateRule(1L, updateDto);

        assertNotNull(result);
        assertEquals("Critical CPU", result.getName());
        assertEquals(90.0, result.getThreshold());
        verify(alertRuleRepository, times(1)).save(any(AlertRule.class));
    }

    //ТЕСТ 5: Обновление несуществующего правила
    @Test
    void testUpdateRuleNotFound() {
        // Проверяем, что при обновлении несуществующего правила выбрасывается исключение
        AlertRuleDto updateDto = AlertRuleDto.builder().name("Test").build();

        when(alertRuleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> alertService.updateRule(999L, updateDto));
    }

    //ТЕСТ 6: Удаление правила
    @Test
    void testDeleteRule() {
        // Проверяем удаление правила
        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));
        doNothing().when(alertRuleRepository).deleteById(1L);

        alertService.deleteRule(1L);

        verify(alertRuleRepository, times(1)).deleteById(1L);
    }

    //ТЕСТ 7: Переключение состояния правила
    @Test
    void testToggleRule() {
        // Проверяем переключение активности правила (активно/неактивно)
        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenReturn(testRule);

        AlertRuleDto result = alertService.toggleRule(1L);

        assertNotNull(result);
        // Проверяем, что состояние переключилось (было true, стало false)
        assertFalse(testRule.getIsActive());
        verify(alertRuleRepository, times(1)).save(any(AlertRule.class));
    }

    //ТЕСТ 8: Получение всех правил
    @Test
    void testGetAllRules() {
        // Проверяем получение списка всех правил
        List<AlertRule> mockRules = Arrays.asList(testRule, testRule);
        when(alertRuleRepository.findAll()).thenReturn(mockRules);

        List<AlertRuleDto> result = alertService.getAllRules();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(alertRuleRepository, times(1)).findAll();
    }

    //ТЕСТ 9: Получение правила по ID
    @Test
    void testGetRuleById() {
        // Проверяем получение правила по его ID
        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));

        AlertRuleDto result = alertService.getRuleById(1L);

        assertNotNull(result);
        assertEquals("High CPU", result.getName());
        verify(alertRuleRepository, times(1)).findById(1L);
    }

    //ТЕСТ 10: Проверка алертов - нарушение условия
    @Test
    void testCheckAlertsViolation() {
        // Проверяем, что при нарушении условия создается событие алерта
        List<AlertRule> activeRules = Collections.singletonList(testRule);
        when(alertRuleRepository.findAllByIsActiveTrue()).thenReturn(activeRules);
        when(alertEventRepository.findAllByRuleIdAndStatus(1L, "triggered"))
                .thenReturn(Collections.emptyList());
        when(alertEventRepository.save(any(AlertEvent.class))).thenReturn(testEvent);

        alertService.checkAlerts(testMetrics);

        verify(alertEventRepository, times(1)).save(any(AlertEvent.class));
    }

    //ТЕСТ 11: Проверка алертов - условие не нарушено
    @Test
    void testCheckAlertsNoViolation() {
        // Проверяем, что при отсутствии нарушения событие не создается
        // Меняем значение метрики на безопасное (ниже порога)
        testMetrics.put("cpu_percent", 50.0);

        List<AlertRule> activeRules = Collections.singletonList(testRule);
        when(alertRuleRepository.findAllByIsActiveTrue()).thenReturn(activeRules);
        when(alertEventRepository.findAllByRuleIdAndStatus(1L, "triggered"))
                .thenReturn(Collections.emptyList());

        alertService.checkAlerts(testMetrics);

        // Проверяем, что save не вызывался
        verify(alertEventRepository, never()).save(any(AlertEvent.class));
    }

    //ТЕСТ 12: Получение активных алертов
    @Test
    void testGetActiveAlerts() {
        // Проверяем получение списка активных (неразрешенных) алертов
        List<AlertEvent> mockEvents = Collections.singletonList(testEvent);
        when(alertEventRepository.findAllByStatusOrderByStartedAtDesc("triggered"))
                .thenReturn(mockEvents);

        List<AlertEventDto> result = alertService.getActiveAlerts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("triggered", result.get(0).getStatus());
        verify(alertEventRepository, times(1))
                .findAllByStatusOrderByStartedAtDesc("triggered");
    }

    //ТЕСТ 13: Получение количества активных алертов
    @Test
    void testGetActiveAlertsCount() {
        // Проверяем подсчет количества активных алертов
        when(alertEventRepository.countByStatus("triggered")).thenReturn(3L);

        long count = alertService.getActiveAlertsCount();

        assertEquals(3L, count);
        verify(alertEventRepository, times(1)).countByStatus("triggered");
    }

    //ТЕСТ 14: Получение всех событий алертов
    @Test
    void testGetAllAlertEvents() {
        // Проверяем получение последних 50 событий
        List<AlertEvent> mockEvents = Arrays.asList(testEvent, testEvent);
        when(alertEventRepository.findLast50Events()).thenReturn(mockEvents);

        List<AlertEventDto> result = alertService.getAllAlertEvents();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(alertEventRepository, times(1)).findLast50Events();
    }
}