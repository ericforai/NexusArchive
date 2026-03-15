// Input: JUnit 5、DateFormat 常量类
// Output: DateFormatTest 测试类
// Pos: 测试模块/常量测试

package com.nexusarchive.common.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DateFormat 常量类单元测试
 *
 * 验证日期格式常量值的正确性和可用性
 */
@DisplayName("DateFormat 常量测试")
@Tag("unit")
class DateFormatTest {

    @Test
    @DisplayName("DATE 常量值应为 'yyyy-MM-dd'")
    void dateConstantShouldBeCorrect() {
        assertEquals("yyyy-MM-dd", DateFormat.DATE);
    }

    @Test
    @DisplayName("DATETIME 常量值应为 'yyyy-MM-dd HH:mm:ss'")
    void dateTimeConstantShouldBeCorrect() {
        assertEquals("yyyy-MM-dd HH:mm:ss", DateFormat.DATETIME);
    }

    @Test
    @DisplayName("ISO_DATETIME 常量值应为 'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    void isoDateTimeConstantShouldBeCorrect() {
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", DateFormat.ISO_DATETIME);
    }

    @Test
    @DisplayName("DATE 格式应能正确解析日期")
    void dateFormatShouldParseValidDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateFormat.DATE);
        String dateStr = "2025-03-15";
        LocalDate date = LocalDate.parse(dateStr, formatter);

        assertEquals(2025, date.getYear());
        assertEquals(3, date.getMonthValue());
        assertEquals(15, date.getDayOfMonth());
    }

    @Test
    @DisplayName("DATETIME 格式应能正确解析日期时间")
    void dateTimeFormatShouldParseValidDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateFormat.DATETIME);
        String dateTimeStr = "2025-03-15 14:30:45";
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);

        assertEquals(2025, dateTime.getYear());
        assertEquals(3, dateTime.getMonthValue());
        assertEquals(15, dateTime.getDayOfMonth());
        assertEquals(14, dateTime.getHour());
        assertEquals(30, dateTime.getMinute());
        assertEquals(45, dateTime.getSecond());
    }

    @Test
    @DisplayName("ISO_DATETIME 格式应符合 ISO 8601 标准")
    void isoDateTimeFormatShouldFollowIso8601() {
        assertTrue(DateFormat.ISO_DATETIME.contains("yyyy-MM-dd"));
        assertTrue(DateFormat.ISO_DATETIME.contains("'T'"));
        assertTrue(DateFormat.ISO_DATETIME.contains("'Z'"));
    }

    @Test
    @DisplayName("常量值不应为空")
    void constantsShouldNotBeBlank() {
        assertFalse(DateFormat.DATE.isBlank());
        assertFalse(DateFormat.DATETIME.isBlank());
        assertFalse(DateFormat.ISO_DATETIME.isBlank());
    }

    @Test
    @DisplayName("所有格式常量应包含 'yyyy-MM-dd' 基础部分")
    void allFormatsShouldContainBaseDatePattern() {
        assertTrue(DateFormat.DATE.contains("yyyy"));
        assertTrue(DateFormat.DATE.contains("MM"));
        assertTrue(DateFormat.DATE.contains("dd"));

        assertTrue(DateFormat.DATETIME.contains("yyyy"));
        assertTrue(DateFormat.DATETIME.contains("MM"));
        assertTrue(DateFormat.DATETIME.contains("dd"));

        assertTrue(DateFormat.ISO_DATETIME.contains("yyyy"));
        assertTrue(DateFormat.ISO_DATETIME.contains("MM"));
        assertTrue(DateFormat.ISO_DATETIME.contains("dd"));
    }

    @Test
    @DisplayName("DATETIME 和 ISO_DATETIME 应包含时间部分")
    void dateTimeFormatsShouldContainTimePattern() {
        assertTrue(DateFormat.DATETIME.contains("HH"));
        assertTrue(DateFormat.DATETIME.contains("mm"));
        assertTrue(DateFormat.DATETIME.contains("ss"));

        assertTrue(DateFormat.ISO_DATETIME.contains("HH"));
        assertTrue(DateFormat.ISO_DATETIME.contains("mm"));
        assertTrue(DateFormat.ISO_DATETIME.contains("ss"));
    }
}
