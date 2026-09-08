package Unit_tests;

import org.junit.jupiter.api.Test;
import ru.student.testing.service.SystemMetricsCollector;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для SystemMetricsCollector.
 * Проверяют сбор системных метрик (требует реальной системы для выполнения).
 */
class SystemMetricsCollectorTest {

    private final SystemMetricsCollector collector = new SystemMetricsCollector();

    //ТЕСТ 1: Сбор всех метрик
    @Test
    void testCollectAllMetrics() {
        // Проверяем, что сбор всех метрик возвращает непустой результат
        Map<String, Double> metrics = collector.collectAllMetrics();

        assertNotNull(metrics);
        assertFalse(metrics.isEmpty());

        // Проверяем, что основные метрики присутствуют
        assertTrue(metrics.containsKey("cpu_percent"));
        assertTrue(metrics.containsKey("ram_used_percent"));
        assertTrue(metrics.containsKey("disk_used_percent"));

        // Проверяем, что значения имеют допустимый диапазон
        assertTrue(metrics.get("cpu_percent") >= 0 && metrics.get("cpu_percent") <= 100);
        assertTrue(metrics.get("ram_used_percent") >= 0 && metrics.get("ram_used_percent") <= 100);
    }
    //ТЕСТ 2: Сбор базовых метрик
    @Test
    void testCollectBasicMetrics() {
        // Проверяем сбор базовых метрик (CPU, RAM)
        Map<String, Double> metrics = collector.collectBasicMetrics();

        assertNotNull(metrics);
        assertTrue(metrics.containsKey("cpu_percent"));
        assertTrue(metrics.containsKey("ram_used_percent"));

        // Проверяем, что значения в допустимом диапазоне
        assertTrue(metrics.get("cpu_percent") >= 0 && metrics.get("cpu_percent") <= 100);
        assertTrue(metrics.get("ram_used_percent") >= 0 && metrics.get("ram_used_percent") <= 100);
    }

    //ТЕСТ 3: Проверка здоровья системы
    @Test
    void testIsSystemHealthy() {
        // Проверяем, что система считается здоровой при успешном сборе метрик
        assertTrue(collector.isSystemHealthy());
    }
}