package ru.student.testing.service;

import lombok.extern.slf4j.Slf4j;
import ru.student.testing.entity.BaseEntity;

/**
 * Абстрактный базовый сервис для всех сервисов в системе.
 * Демонстрирует использование принципа наследования в ООП.
 * Предоставляет общие методы для логирования операций с сущностями
 * и вспомогательные методы для оценки состояния системы.
 */
@Slf4j
public abstract class BaseService<T extends BaseEntity> {

    /**
     * Логирует создание новой сущности.
     * Использует метод getEntityDisplayName() для получения читаемого имени.
     */
    protected void logCreation(T entity) {
        log.info("Создана сущность: {}", entity.getEntityDisplayName());
    }

    /**
     * Логирует обновление существующей сущности.
     */
    protected void logUpdate(T entity) {
        log.info("Обновлена сущность: {}", entity.getEntityDisplayName());
    }

    /**
     * Логирует удаление сущности.
     */
    protected void logDeletion(T entity) {
        log.info("🗑Удалена сущность: {}", entity.getEntityDisplayName());
    }

    /**
     * Абстрактный метод для получения типа сущности.
     * Должен быть реализован в каждом наследнике.
     */
    public abstract String getEntityType();

    /**
     * Проверяет, является ли значение критическим (превышает порог).
     * Используется для оценки состояния метрик.
     */
    protected boolean isCriticalValue(double value, double threshold) {
        return value > threshold;
    }

    /**
     * Определяет статус здоровья системы на основе значения метрики.
     * Использует два порога: предупреждение и критический.
     */
    protected String getHealthStatus(double value, double warningThreshold, double criticalThreshold) {
        if (value > criticalThreshold) {
            return "CRITICAL";
        } else if (value > warningThreshold) {
            return "WARNING";
        } else {
            return "OK";
        }
    }

    /**
     * Проверяет, находится ли значение в допустимом диапазоне.
     */
    protected boolean isValueInRange(Double value, Double min, Double max) {
        if (value == null) {
            return false;
        }
        boolean minOk = min == null || value >= min;
        boolean maxOk = max == null || value <= max;
        return minOk && maxOk;
    }

    /**
     * Округляет значение до указанного количества знаков после запятой.
     */
    protected double roundValue(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException("Количество знаков должно быть >= 0");
        }
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    /**
     * Проверяет, превышает ли значение допустимый процент от порога.
     */
    protected boolean isExceededPercent(Double currentValue, Double threshold, Double tolerancePercent) {
        if (currentValue == null || threshold == null || tolerancePercent == null) {
            return false;
        }
        double maxAllowed = threshold * (1 + tolerancePercent / 100);
        return currentValue > maxAllowed;
    }

    /**
     * Проверяет, упало ли значение ниже допустимого процента от порога.
     */
    protected boolean isDroppedBelowPercent(Double currentValue, Double threshold, Double tolerancePercent) {
        if (currentValue == null || threshold == null || tolerancePercent == null) {
            return false;
        }
        double minAllowed = threshold * (1 - tolerancePercent / 100);
        return currentValue < minAllowed;
    }

    /**
     * Получает процентное значение из числа.
     * Преобразует значение из формата 0-1 в 0-100%.
     */
    protected double toPercent(double value) {
        return value * 100;
    }

    /**
     * Преобразует байты в мегабайты.
     */
    protected double bytesToMb(long bytes) {
        return (double) bytes / 1024 / 1024;
    }

    /**
     * Преобразует байты в гигабайты.
     */
    protected double bytesToGb(long bytes) {
        return (double) bytes / 1024 / 1024 / 1024;
    }
}