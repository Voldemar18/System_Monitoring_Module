package Unit_tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.student.testing.dto.MetricDto;
import ru.student.testing.entity.Metric;
import ru.student.testing.repository.MetricRepository;
import ru.student.testing.service.MetricServiceImpl;
import ru.student.testing.service.SystemMetricsCollector;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для MetricServiceImpl.
 * Проверяют основные методы работы с метриками: сохранение, получение, отчеты.
 */
@ExtendWith(MockitoExtension.class)
class MetricServiceImplTest {

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private SystemMetricsCollector metricsCollector;

    @InjectMocks
    private MetricServiceImpl metricService;

    private Metric testMetric;
    private Map<String, Double> testMetricsMap;

    @BeforeEach
    void setUp() {
        // Подготовка тестовых данных перед каждым тестом
        testMetric = new Metric("localhost", "cpu_percent", 45.5);
        testMetric.setId(1L);
        testMetric.initAuditFields();

        testMetricsMap = new HashMap<>();
        testMetricsMap.put("cpu_percent", 45.5);
        testMetricsMap.put("ram_used_percent", 60.0);
        testMetricsMap.put("disk_used_percent", 75.0);
    }

    //ТЕСТ 1: Проверка типа сущности ===
    @Test
    void testGetEntityType() {
        // Проверяем, что сервис возвращает правильный тип сущности
        assertEquals("Metric", metricService.getEntityType());
    }

    //ТЕСТ 2: Сохранение одной метрики
    @Test
    void testSaveMetric() {
        // Проверяем сохранение одной метрики и возврат DTO
        when(metricRepository.save(any(Metric.class))).thenReturn(testMetric);

        MetricDto result = metricService.saveMetric("cpu_percent", 45.5);

        assertNotNull(result);
        assertEquals("cpu_percent", result.getMetricName());
        assertEquals(45.5, result.getValue());
        verify(metricRepository, times(1)).save(any(Metric.class));
    }

    //ТЕСТ 3: Сохранение нескольких метрик
    @Test
    void testSaveMetrics() {
        // Проверяем сохранение набора метрик
        when(metricRepository.save(any(Metric.class))).thenReturn(testMetric);

        metricService.saveMetrics(testMetricsMap);

        // Проверяем, что save() был вызван 3 раза (по числу метрик)
        verify(metricRepository, times(3)).save(any(Metric.class));
    }

    //ТЕСТ 4: Получение последнего значения метрики
    @Test
    void testGetLatestValue() {
        // Проверяем получение последнего значения существующей метрики
        when(metricRepository.findLatestValueByMetricName("cpu_percent")).thenReturn(45.5);

        Double result = metricService.getLatestValue("cpu_percent");

        assertNotNull(result);
        assertEquals(45.5, result);
        verify(metricRepository, times(1)).findLatestValueByMetricName("cpu_percent");
    }

    //ТЕСТ 5: Получение последнего значения несуществующей метрики
    @Test
    void testGetLatestValueNotFound() {
        // Проверяем, что для несуществующей метрики возвращается null
        when(metricRepository.findLatestValueByMetricName("unknown")).thenReturn(null);

        Double result = metricService.getLatestValue("unknown");

        assertNull(result);
    }
    //ТЕСТ 6: Получение последних N записей
    @Test
    void testGetLatestMetrics() {
        // Проверяем получение последних 10 записей
        List<Metric> mockMetrics = Arrays.asList(testMetric, testMetric);
        when(metricRepository.findTop100ByMetricNameOrderByTimestampDesc("cpu_percent"))
                .thenReturn(mockMetrics);

        List<MetricDto> result = metricService.getLatestMetrics("cpu_percent", 10);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(metricRepository, times(1))
                .findTop100ByMetricNameOrderByTimestampDesc("cpu_percent");
    }

    //ТЕСТ 7: Получение списка всех названий метрик
    @Test
    void testGetAllMetricNames() {
        // Проверяем получение списка всех названий метрик
        List<String> mockNames = Arrays.asList("cpu_percent", "ram_used_percent", "disk_used_percent");
        when(metricRepository.findAllMetricNames()).thenReturn(mockNames);

        List<String> result = metricService.getAllMetricNames();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("cpu_percent"));
        verify(metricRepository, times(1)).findAllMetricNames();
    }

    //ТЕСТ 8: Сбор и сохранение метрик
    @Test
    void testCollectAndSaveMetrics() {
        // Проверяем сбор метрик через SystemMetricsCollector и их сохранение
        when(metricsCollector.collectAllMetrics()).thenReturn(testMetricsMap);
        when(metricRepository.save(any(Metric.class))).thenReturn(testMetric);

        metricService.collectAndSaveMetrics();

        verify(metricsCollector, times(1)).collectAllMetrics();
        verify(metricRepository, times(3)).save(any(Metric.class));
    }

    //ТЕСТ 9: Получение базовых метрик
    @Test
    void testGetBasicMetrics() {
        // Проверяем получение базовых метрик (CPU, RAM)
        Map<String, Double> basicMetrics = new HashMap<>();
        basicMetrics.put("cpu_percent", 45.5);
        basicMetrics.put("ram_used_percent", 60.0);
        when(metricsCollector.collectBasicMetrics()).thenReturn(basicMetrics);

        Map<String, Double> result = metricService.getBasicMetrics();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("cpu_percent"));
        assertTrue(result.containsKey("ram_used_percent"));
        verify(metricsCollector, times(1)).collectBasicMetrics();
    }

    //ТЕСТ 10: Очистка старых метрик
    @Test
    void testCleanOldMetrics() {
        // Проверяем удаление метрик старше N дней
        doNothing().when(metricRepository).deleteOlderThan(30);

        metricService.cleanOldMetrics(30);

        verify(metricRepository, times(1)).deleteOlderThan(30);
    }

    //ТЕСТ 11: Генерация отчета (проверка что отчет не пустой)
    @Test
    void testGenerateSystemReport() {
        // Проверяем, что отчет генерируется и содержит ключевые секции
        List<Object[]> mockLatest = new ArrayList<>();
        mockLatest.add(new Object[]{"cpu_percent", 45.5, LocalDateTime.now()});
        mockLatest.add(new Object[]{"ram_used_percent", 60.0, LocalDateTime.now()});
        mockLatest.add(new Object[]{"disk_used_percent", 75.0, LocalDateTime.now()});

        when(metricRepository.findLatestEachMetric()).thenReturn(mockLatest);

        String report = metricService.generateSystemReport();

        assertNotNull(report);
        assertFalse(report.isEmpty());
        assertTrue(report.contains("СИСТЕМНЫЙ ОТЧЕТ"));
        assertTrue(report.contains("CPU"));
        assertTrue(report.contains("RAM"));
        assertTrue(report.contains("Disk"));
    }

    // ТЕСТ 12: Получение всех последних метрик
    @Test
    void testGetLatestAllMetrics() {
        // Проверяем получение последних значений всех метрик
        List<Object[]> mockLatest = new ArrayList<>();
        mockLatest.add(new Object[]{"cpu_percent", 45.5, LocalDateTime.now()});
        mockLatest.add(new Object[]{"ram_used_percent", 60.0, LocalDateTime.now()});

        when(metricRepository.findLatestEachMetric()).thenReturn(mockLatest);

        Map<String, Double> result = metricService.getLatestAllMetrics();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("cpu_percent"));
        assertEquals(45.5, result.get("cpu_percent"));
    }
}