package ru.student.testing.service;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Компонент для сбора системных метрик с помощью библиотеки OSHI
 */
@Component
public class SystemMetricsCollector {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final OperatingSystem operatingSystem;

    public SystemMetricsCollector() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.operatingSystem = systemInfo.getOperatingSystem();
    }

    /**
     * Собрать все метрики системы
     * @return Map<String, Double> где ключ - имя метрики, значение - число
     */
    public Map<String, Double> collectAllMetrics() {
        Map<String, Double> metrics = new HashMap<>();

        // Сбор CPU метрик
        collectCpuMetrics(metrics);

        // Сбор RAM метрик
        collectRamMetrics(metrics);

        // Сбор Disk метрик
        collectDiskMetrics(metrics);

        // Сбор Network метрик (опционально)
        collectNetworkMetrics(metrics);

        return metrics;
    }

    /**
     * Сбор метрик CPU
     */
    private void collectCpuMetrics(Map<String, Double> metrics) {
        CentralProcessor cpu = hardware.getProcessor();

        // Загрузка CPU в процентах (берем среднее за 1 секунду)
        double cpuLoad = cpu.getSystemCpuLoad(1000) * 100;
        metrics.put("cpu_percent", Math.round(cpuLoad * 10.0) / 10.0);

        // Load Average (1, 5, 15 минут)
        double[] loadAverage = cpu.getSystemLoadAverage(3);
        if (loadAverage.length >= 1) {
            metrics.put("load_avg_1min", Math.round(loadAverage[0] * 10.0) / 10.0);
        }
        if (loadAverage.length >= 2) {
            metrics.put("load_avg_5min", Math.round(loadAverage[1] * 10.0) / 10.0);
        }
        if (loadAverage.length >= 3) {
            metrics.put("load_avg_15min", Math.round(loadAverage[2] * 10.0) / 10.0);
        }
    }

    /**
     * Сбор метрик RAM
     */
    private void collectRamMetrics(Map<String, Double> metrics) {
        GlobalMemory memory = hardware.getMemory();

        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;

        // В MB для удобства
        double totalMemoryMB = (double) totalMemory / 1024 / 1024;
        double usedMemoryMB = (double) usedMemory / 1024 / 1024;
        double availableMemoryMB = (double) availableMemory / 1024 / 1024;

        metrics.put("ram_total_mb", Math.round(totalMemoryMB * 10.0) / 10.0);
        metrics.put("ram_used_mb", Math.round(usedMemoryMB * 10.0) / 10.0);
        metrics.put("ram_available_mb", Math.round(availableMemoryMB * 10.0) / 10.0);

        // Процент использования
        double usedPercent = ((double) usedMemory / totalMemory) * 100;
        metrics.put("ram_used_percent", Math.round(usedPercent * 10.0) / 10.0);
    }

    /**
     * Сбор метрик диска
     */
    private void collectDiskMetrics(Map<String, Double> metrics) {
        FileSystem fileSystem = operatingSystem.getFileSystem();
        OSFileStore[] fileStores = fileSystem.getFileStores();

        long totalSpace = 0;
        long usedSpace = 0;

        for (OSFileStore fs : fileStores) {
            // Пропускаем виртуальные файловые системы (Linux)
            if (fs.getType().equals("tmpfs") || fs.getType().equals("devtmpfs")) {
                continue;
            }
            totalSpace += fs.getTotalSpace();
            usedSpace += fs.getTotalSpace() - fs.getUsableSpace();
        }

        // В GB для удобства
        double totalSpaceGB = (double) totalSpace / 1024 / 1024 / 1024;
        double usedSpaceGB = (double) usedSpace / 1024 / 1024 / 1024;

        metrics.put("disk_total_gb", Math.round(totalSpaceGB * 10.0) / 10.0);
        metrics.put("disk_used_gb", Math.round(usedSpaceGB * 10.0) / 10.0);

        if (totalSpace > 0) {
            double usedPercent = ((double) usedSpace / totalSpace) * 100;
            metrics.put("disk_used_percent", Math.round(usedPercent * 10.0) / 10.0);
        }
    }

    /**
     * Сбор метрик сети (опционально)
     */
    private void collectNetworkMetrics(Map<String, Double> metrics) {
        // OSHI не предоставляет простой способ получить скорость сети
        // Можно использовать Java Management Extensions (JMX) или оставить как заглушку
        // Для простоты оставим это место для будущего расширения
        metrics.put("network_rx_mb", 0.0);
        metrics.put("network_tx_mb", 0.0);
    }

    /**
     * Получить только основные метрики (для быстрых обновлений)
     */
    public Map<String, Double> collectBasicMetrics() {
        Map<String, Double> metrics = new HashMap<>();

        CentralProcessor cpu = hardware.getProcessor();
        double cpuLoad = cpu.getSystemCpuLoad(1000) * 100;
        metrics.put("cpu_percent", Math.round(cpuLoad * 10.0) / 10.0);

        GlobalMemory memory = hardware.getMemory();
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        double usedPercent = ((double) (totalMemory - availableMemory) / totalMemory) * 100;
        metrics.put("ram_used_percent", Math.round(usedPercent * 10.0) / 10.0);

        return metrics;
    }

    /**
     * Проверка доступности системы (health check)
     */
    public boolean isSystemHealthy() {
        try {
            collectBasicMetrics();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}