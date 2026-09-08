package ru.student.testing.service;

import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Компонент для сбора системных метрик с использованием библиотеки OSHI.
 * Собирает показатели CPU, RAM, диска и сети.
 */
@Slf4j
@Component
public class SystemMetricsCollector {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final OperatingSystem operatingSystem;

    /**
     * Хранилище для предыдущих значений сетевых байтов.
     * Используется для расчета скорости передачи данных.
     */
    private final Map<String, Long> lastNetworkBytes = new ConcurrentHashMap<>();

    /**
     * Время последнего замера сетевой статистики.
     */
    private long lastNetworkCheckTime = System.currentTimeMillis();

    /**
     * Конструктор. Инициализирует OSHI компоненты.
     */
    public SystemMetricsCollector() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.operatingSystem = systemInfo.getOperatingSystem();
        log.info("SystemMetricsCollector инициализирован");
        logNetworkInterfaces(); // Логируем сетевые интерфейсы при старте
    }

    /**
     * Собирает все доступные системные метрики.
     */
    public Map<String, Double> collectAllMetrics() {
        Map<String, Double> metrics = new HashMap<>();

        collectCpuMetrics(metrics);
        collectRamMetrics(metrics);
        collectDiskMetrics(metrics);
        collectNetworkMetrics(metrics);

        return metrics;
    }

    /**
     * Собирает только базовые метрики (CPU и RAM).
     * Используется для быстрой проверки состояния системы.
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
     * Проверяет, работоспособна ли система (может ли собрать метрики).
     */
    public boolean isSystemHealthy() {
        try {
            collectBasicMetrics();
            return true;
        } catch (Exception e) {
            log.error("Ошибка при проверке здоровья системы: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Логирует все обнаруженные сетевые интерфейсы.
     * Полезно для отладки сетевых метрик.
     */
    public void logNetworkInterfaces() {
        try {
            List<NetworkIF> networkIFs = hardware.getNetworkIFs();
            log.info("=== Обнаружено {} сетевых интерфейсов ===", networkIFs.size());
            for (NetworkIF net : networkIFs) {
                String ipv4 = net.getIPv4addr().length > 0 ? net.getIPv4addr()[0] : "N/A";
                boolean isLoopback = net.getName() != null && net.getName().equals("lo");
                log.info("  Интерфейс: {} | MAC: {} | IPv4: {} | Loopback: {}",
                        net.getName(),
                        net.getMacaddr(),
                        ipv4,
                        isLoopback
                );
            }
            log.info("Конец списка сетевых интерфейсов");
        } catch (Exception e) {
            log.warn("Не удалось получить информацию о сетевых интерфейсах: {}", e.getMessage());
        }
    }
    /**
     * Собирает метрики CPU:
     * - cpu_percent: загрузка CPU в процентах
     * - load_avg_1min, load_avg_5min, load_avg_15min: средняя нагрузка
     */
    private void collectCpuMetrics(Map<String, Double> metrics) {
        try {
            CentralProcessor cpu = hardware.getProcessor();

            // Загрузка CPU (0-100%)
            double cpuLoad = cpu.getSystemCpuLoad(1000) * 100;
            if (cpuLoad >= 0) {
                metrics.put("cpu_percent", Math.round(cpuLoad * 10.0) / 10.0);
            }

            // Load Average (количество процессов в очереди)
            double[] loadAverage = cpu.getSystemLoadAverage(3);
            if (loadAverage.length >= 1 && loadAverage[0] >= 0) {
                metrics.put("load_avg_1min", Math.round(loadAverage[0] * 100.0) / 100.0);
            }
            if (loadAverage.length >= 2 && loadAverage[1] >= 0) {
                metrics.put("load_avg_5min", Math.round(loadAverage[1] * 100.0) / 100.0);
            }
            if (loadAverage.length >= 3 && loadAverage[2] >= 0) {
                metrics.put("load_avg_15min", Math.round(loadAverage[2] * 100.0) / 100.0);
            }
        } catch (Exception e) {
            log.warn("Ошибка при сборе CPU метрик: {}", e.getMessage());
        }
    }

    /**
     * Собирает метрики RAM:
     * - ram_total_mb: общий объем RAM в MB
     * - ram_used_mb: использованный объем RAM в MB
     * - ram_available_mb: доступный объем RAM в MB
     * - ram_used_percent: процент использованной RAM
     */
    private void collectRamMetrics(Map<String, Double> metrics) {
        try {
            GlobalMemory memory = hardware.getMemory();

            long totalMemory = memory.getTotal();
            long availableMemory = memory.getAvailable();
            long usedMemory = totalMemory - availableMemory;

            double totalMemoryMB = (double) totalMemory / 1024 / 1024;
            double usedMemoryMB = (double) usedMemory / 1024 / 1024;
            double availableMemoryMB = (double) availableMemory / 1024 / 1024;

            metrics.put("ram_total_mb", Math.round(totalMemoryMB * 10.0) / 10.0);
            metrics.put("ram_used_mb", Math.round(usedMemoryMB * 10.0) / 10.0);
            metrics.put("ram_available_mb", Math.round(availableMemoryMB * 10.0) / 10.0);

            if (totalMemory > 0) {
                double usedPercent = ((double) usedMemory / totalMemory) * 100;
                metrics.put("ram_used_percent", Math.round(usedPercent * 10.0) / 10.0);
            }
        } catch (Exception e) {
            log.warn("Ошибка при сборе RAM метрик: {}", e.getMessage());
        }
    }

    /**
     * Собирает метрики диска:
     * - disk_total_gb: общий объем диска в GB
     * - disk_used_gb: использованный объем диска в GB
     * - disk_used_percent: процент использованного диска
     */
    private void collectDiskMetrics(Map<String, Double> metrics) {
        try {
            FileSystem fileSystem = operatingSystem.getFileSystem();
            List<OSFileStore> fileStores = fileSystem.getFileStores();

            long totalSpace = 0;
            long usedSpace = 0;

            for (OSFileStore fs : fileStores) {
                String type = fs.getType();
                if (type == null || type.equals("tmpfs") || type.equals("devtmpfs") || type.equals("squashfs")) {
                    continue;
                }
                if (fs.getName() != null && fs.getName().startsWith("//")) {
                    continue;
                }
                totalSpace += fs.getTotalSpace();
                usedSpace += fs.getTotalSpace() - fs.getUsableSpace();
            }

            if (totalSpace > 0) {
                double totalSpaceGB = (double) totalSpace / 1024 / 1024 / 1024;
                double usedSpaceGB = (double) usedSpace / 1024 / 1024 / 1024;
                double usedPercent = ((double) usedSpace / totalSpace) * 100;

                metrics.put("disk_total_gb", Math.round(totalSpaceGB * 10.0) / 10.0);
                metrics.put("disk_used_gb", Math.round(usedSpaceGB * 10.0) / 10.0);
                metrics.put("disk_used_percent", Math.round(usedPercent * 10.0) / 10.0);
            }
        } catch (Exception e) {
            log.warn("Ошибка при сборе дисковых метрик: {}", e.getMessage());
        }
    }

    /**
     * Собирает метрики сети:
     * - network_rx_mb: скорость входящего трафика в MB/s
     * - network_tx_mb: скорость исходящего трафика в MB/s
     * Алгоритм: запоминает общее количество переданных байт на каждом замере,
     * и вычисляет скорость как разницу байт за прошедший промежуток времени.
     */
    private void collectNetworkMetrics(Map<String, Double> metrics) {
        try {
            // Получаем список сетевых интерфейсов
            List<NetworkIF> networkIFs = hardware.getNetworkIFs();
            long currentTime = System.currentTimeMillis();
            long timeDelta = currentTime - lastNetworkCheckTime;

            long totalRxBytes = 0;
            long totalTxBytes = 0;
            int activeInterfaces = 0;

            // Суммируем все активные интерфейсы (исключая loopback)
            for (NetworkIF net : networkIFs) {
                String name = net.getName();
                if (name == null || name.equals("lo") || name.equals("Loopback")) {
                    continue;
                }

                long bytesRecv = net.getBytesRecv();
                long bytesSent = net.getBytesSent();

                // Проверяем, что интерфейс активен (передает данные)
                if (bytesRecv > 0 || bytesSent > 0) {
                    totalRxBytes += bytesRecv;
                    totalTxBytes += bytesSent;
                    activeInterfaces++;
                }
            }

            // Если нет активных интерфейсов, пробуем использовать первый попавшийся
            if (activeInterfaces == 0 && !networkIFs.isEmpty()) {
                NetworkIF fallback = networkIFs.get(0);
                totalRxBytes = fallback.getBytesRecv();
                totalTxBytes = fallback.getBytesSent();
                log.debug("Используем fallback интерфейс: {}", fallback.getName());
            }

            String key = "network_total";

            // Если есть предыдущие значения и прошло достаточно времени (> 1 секунды)
            if (lastNetworkBytes.containsKey(key + "_rx") &&
                    lastNetworkBytes.containsKey(key + "_tx") &&
                    timeDelta > 1000) {

                long prevRx = lastNetworkBytes.get(key + "_rx");
                long prevTx = lastNetworkBytes.get(key + "_tx");

                long deltaRx = totalRxBytes - prevRx;
                long deltaTx = totalTxBytes - prevTx;

                if (deltaRx < 0) deltaRx = 0;
                if (deltaTx < 0) deltaTx = 0;

                double rxSpeedBps = (double) deltaRx / timeDelta * 1000;
                double txSpeedBps = (double) deltaTx / timeDelta * 1000;

                double rxSpeedMBps = rxSpeedBps / 1024 / 1024;
                double txSpeedMBps = txSpeedBps / 1024 / 1024;

                metrics.put("network_rx_mb", Math.round(rxSpeedMBps * 100.0) / 100.0);
                metrics.put("network_tx_mb", Math.round(txSpeedMBps * 100.0) / 100.0);

                if (log.isDebugEnabled()) {
                    log.debug("Сеть: RX={} MB/s, TX={} MB/s, deltaTime={}ms, интерфейсов={}",
                            rxSpeedMBps, txSpeedMBps, timeDelta, activeInterfaces);
                }
            } else {
                metrics.put("network_rx_mb", 0.0);
                metrics.put("network_tx_mb", 0.0);
                if (log.isDebugEnabled()) {
                    log.debug("Первый замер сети, ожидаем следующий для расчета скорости");
                }
            }

            lastNetworkBytes.put(key + "_rx", totalRxBytes);
            lastNetworkBytes.put(key + "_tx", totalTxBytes);
            lastNetworkCheckTime = currentTime;

        } catch (Exception e) {
            log.warn("Ошибка при сборе сетевых метрик: {}", e.getMessage());
            metrics.put("network_rx_mb", 0.0);
            metrics.put("network_tx_mb", 0.0);
        }
    }
}