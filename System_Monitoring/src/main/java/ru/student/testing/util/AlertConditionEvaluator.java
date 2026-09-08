package ru.student.testing.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

@Component
public class AlertConditionEvaluator {

    private static final Map<String, BiPredicate<Double, Double>> OPERATIONS = new HashMap<>();

    static {
        OPERATIONS.put(">", (current, threshold) -> current > threshold);
        OPERATIONS.put("<", (current, threshold) -> current < threshold);
        OPERATIONS.put(">=", (current, threshold) -> current >= threshold);
        OPERATIONS.put("<=", (current, threshold) -> current <= threshold);
        OPERATIONS.put("==", Double::equals);
        OPERATIONS.put("!=", (current, threshold) -> !current.equals(threshold));
    }

    public boolean isViolated(Double currentValue, String condition, Double threshold) {
        if (currentValue == null || threshold == null) {
            return false;
        }

        BiPredicate<Double, Double> operation = OPERATIONS.get(condition);
        if (operation == null) {
            throw new IllegalArgumentException("Неподдерживаемое условие: " + condition);
        }

        return operation.test(currentValue, threshold);
    }

    public boolean isRecovered(Double currentValue, String condition, Double threshold) {
        return !isViolated(currentValue, condition, threshold);
    }

    public String getConditionDescription(String condition, Double threshold) {
        return switch (condition) {
            case ">" -> "больше " + threshold;
            case "<" -> "меньше " + threshold;
            case ">=" -> "больше или равно " + threshold;
            case "<=" -> "меньше или равно " + threshold;
            case "==" -> "равно " + threshold;
            case "!=" -> "не равно " + threshold;
            default -> condition + " " + threshold;
        };
    }

    public boolean isValidCondition(String condition) {
        return OPERATIONS.containsKey(condition);
    }

    public String[] getAvailableConditions() {
        return OPERATIONS.keySet().toArray(String[]::new);
    }

    public boolean isValueInRange(Double value, Double min, Double max) {
        if (value == null) {
            return false;
        }
        return (min == null || value >= min) && (max == null || value <= max);
    }

    public double roundValue(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException("Количество знаков должно быть >= 0");
        }
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    public boolean isExceededPercent(Double currentValue, Double threshold, Double tolerancePercent) {
        if (currentValue == null || threshold == null || tolerancePercent == null) {
            return false;
        }
        double maxAllowed = threshold * (1 + tolerancePercent / 100);
        return currentValue > maxAllowed;
    }

    public boolean isDroppedBelowPercent(Double currentValue, Double threshold, Double tolerancePercent) {
        if (currentValue == null || threshold == null || tolerancePercent == null) {
            return false;
        }
        double minAllowed = threshold * (1 - tolerancePercent / 100);
        return currentValue < minAllowed;
    }
}