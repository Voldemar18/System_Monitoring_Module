package Unit_tests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.student.testing.util.AlertConditionEvaluator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-тесты для AlertConditionEvaluator.
 * Проверяют все операции сравнения: >, <, >=, <=, ==, !=
 */
class AlertConditionEvaluatorTest {

    private AlertConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new AlertConditionEvaluator();
    }

    //ТЕСТ 1: Проверка условия ">"
    @Test
    void testIsViolatedGreaterThan() {
        // Проверяем, что 85 > 80 = true
        assertTrue(evaluator.isViolated(85.0, ">", 80.0));
        // Проверяем, что 75 > 80 = false
        assertFalse(evaluator.isViolated(75.0, ">", 80.0));
        // Проверяем, что 80 > 80 = false (равенство)
        assertFalse(evaluator.isViolated(80.0, ">", 80.0));
    }

    //ТЕСТ 2: Проверка условия "<"
    @Test
    void testIsViolatedLessThan() {
        // Проверяем, что 75 < 80 = true
        assertTrue(evaluator.isViolated(75.0, "<", 80.0));
        // Проверяем, что 85 < 80 = false
        assertFalse(evaluator.isViolated(85.0, "<", 80.0));
        // Проверяем, что 80 < 80 = false
        assertFalse(evaluator.isViolated(80.0, "<", 80.0));
    }

    //ТЕСТ 3: Проверка условия ">="
    @Test
    void testIsViolatedGreaterOrEqual() {
        // Проверяем, что 85 >= 80 = true
        assertTrue(evaluator.isViolated(85.0, ">=", 80.0));
        // Проверяем, что 80 >= 80 = true (равенство)
        assertTrue(evaluator.isViolated(80.0, ">=", 80.0));
        // Проверяем, что 75 >= 80 = false
        assertFalse(evaluator.isViolated(75.0, ">=", 80.0));
    }

    //ТЕСТ 4: Проверка условия "<="
    @Test
    void testIsViolatedLessOrEqual() {
        // Проверяем, что 75 <= 80 = true
        assertTrue(evaluator.isViolated(75.0, "<=", 80.0));
        // Проверяем, что 80 <= 80 = true (равенство)
        assertTrue(evaluator.isViolated(80.0, "<=", 80.0));
        // Проверяем, что 85 <= 80 = false
        assertFalse(evaluator.isViolated(85.0, "<=", 80.0));
    }

    //ТЕСТ 5: Проверка условия "=="
    @Test
    void testIsViolatedEqual() {
        // Проверяем, что 80 == 80 = true
        assertTrue(evaluator.isViolated(80.0, "==", 80.0));
        // Проверяем, что 85 == 80 = false
        assertFalse(evaluator.isViolated(85.0, "==", 80.0));
    }

    //ТЕСТ 6: Проверка условия "!="
    @Test
    void testIsViolatedNotEqual() {
        // Проверяем, что 85 != 80 = true
        assertTrue(evaluator.isViolated(85.0, "!=", 80.0));
        // Проверяем, что 80 != 80 = false
        assertFalse(evaluator.isViolated(80.0, "!=", 80.0));
    }

    //ТЕСТ 7: Проверка восстановления (recovered)
    @Test
    void testIsRecovered() {
        // Проверяем, что если 75 > 80 = false, то recovered = true
        assertTrue(evaluator.isRecovered(75.0, ">", 80.0));
        // Проверяем, что если 85 > 80 = true, то recovered = false
        assertFalse(evaluator.isRecovered(85.0, ">", 80.0));
    }

    //ТЕСТ 8: Обработка null значений
    @Test
    void testNullValues() {
        // При null должно возвращаться false
        assertFalse(evaluator.isViolated(null, ">", 80.0));
        assertFalse(evaluator.isViolated(85.0, ">", null));
        assertFalse(evaluator.isViolated(null, ">", null));
    }

    //ТЕСТ 9: Проверка несуществующего условия
    @Test
    void testInvalidCondition() {
        // При несуществующем условии должно выбрасываться исключение
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.isViolated(85.0, "??", 80.0));
    }

    //ТЕСТ 10: Проверка валидности условия
    @Test
    void testIsValidCondition() {
        // Проверяем, что стандартные условия валидны
        assertTrue(evaluator.isValidCondition(">"));
        assertTrue(evaluator.isValidCondition("<"));
        assertTrue(evaluator.isValidCondition(">="));
        assertTrue(evaluator.isValidCondition("<="));
        assertTrue(evaluator.isValidCondition("=="));
        assertTrue(evaluator.isValidCondition("!="));
        // Проверяем, что нестандартное условие невалидно
        assertFalse(evaluator.isValidCondition("??"));
    }

    //ТЕСТ 11: Получение описания условия
    @Test
    void testGetConditionDescription() {
        // Проверяем, что описание условия формируется корректно
        assertEquals("больше 80.0", evaluator.getConditionDescription(">", 80.0));
        assertEquals("меньше 80.0", evaluator.getConditionDescription("<", 80.0));
        assertEquals("больше или равно 80.0", evaluator.getConditionDescription(">=", 80.0));
        assertEquals("меньше или равно 80.0", evaluator.getConditionDescription("<=", 80.0));
        assertEquals("равно 80.0", evaluator.getConditionDescription("==", 80.0));
        assertEquals("не равно 80.0", evaluator.getConditionDescription("!=", 80.0));
    }

    //ТЕСТ 12: Проверка нахождения значения в диапазоне
    @Test
    void testIsValueInRange() {
        // Проверяем, что 50 находится в диапазоне [0, 100]
        assertTrue(evaluator.isValueInRange(50.0, 0.0, 100.0));
        // Проверяем, что 150 НЕ находится в диапазоне [0, 100]
        assertFalse(evaluator.isValueInRange(150.0, 0.0, 100.0));
        // Проверяем, что при null возвращается false
        assertFalse(evaluator.isValueInRange(null, 0.0, 100.0));
        // Проверяем, что без нижней границы работает корректно
        assertTrue(evaluator.isValueInRange(50.0, null, 100.0));
        // Проверяем, что без верхней границы работает корректно
        assertTrue(evaluator.isValueInRange(50.0, 0.0, null));
    }

    //ТЕСТ 13: Проверка округления
    @Test
    void testRoundValue() {
        // Проверяем округление до 2 знаков
        assertEquals(3.14, evaluator.roundValue(3.14159, 2));
        // Проверяем округление до 0 знаков
        assertEquals(3.0, evaluator.roundValue(3.14159, 0));
        // Проверяем, что при отрицательном количестве знаков выбрасывается исключение
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.roundValue(3.14159, -1));
    }

    //ТЕСТ 14: Проверка превышения процента
    @Test
    void testIsExceededPercent() {
        // Проверяем, что 110 превышает 100 на 10%
        assertTrue(evaluator.isExceededPercent(110.0, 100.0, 5.0));
        // Проверяем, что 104 НЕ превышает 100 на 5%
        assertFalse(evaluator.isExceededPercent(104.0, 100.0, 5.0));
    }

    //ТЕСТ 15: Проверка падения ниже процента
    @Test
    void testIsDroppedBelowPercent() {
        // Проверяем, что 90 упало ниже 100 на 10%
        assertTrue(evaluator.isDroppedBelowPercent(90.0, 100.0, 5.0));
        // Проверяем, что 96 НЕ упало ниже 100 на 5%
        assertFalse(evaluator.isDroppedBelowPercent(96.0, 100.0, 5.0));
    }
}