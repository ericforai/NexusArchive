// Input: JUnit 5、AssertJ、Jackson、ArchiveStatus
// Output: ArchiveStatusTest 测试类
// Pos: 测试

package com.nexusarchive.common.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * ArchiveStatus 枚举测试
 * <p>遵循 TDD 原则：先写测试，定义期望行为
 */
@DisplayName("ArchiveStatus 枚举测试")
class ArchiveStatusTest {

    @Test
    @DisplayName("应该支持所有现有状态值")
    void shouldSupportAllExistingStatusValues() {
        assertThat(ArchiveStatus.fromCode("draft")).isEqualTo(ArchiveStatus.DRAFT);
        assertThat(ArchiveStatus.fromCode("pending")).isEqualTo(ArchiveStatus.PENDING);
        assertThat(ArchiveStatus.fromCode("archived")).isEqualTo(ArchiveStatus.ARCHIVED);
    }

    @Test
    @DisplayName("应该支持大小写不敏感的反序列化")
    void shouldBeCaseInsensitive() {
        assertThat(ArchiveStatus.fromCode("DRAFT")).isEqualTo(ArchiveStatus.DRAFT);
        assertThat(ArchiveStatus.fromCode("Draft")).isEqualTo(ArchiveStatus.DRAFT);
        assertThat(ArchiveStatus.fromCode("PENDING")).isEqualTo(ArchiveStatus.PENDING);
        assertThat(ArchiveStatus.fromCode("Pending")).isEqualTo(ArchiveStatus.PENDING);
    }

    @Test
    @DisplayName("null 或空字符串应该返回默认值 DRAFT")
    void shouldReturnDefaultForNullOrEmpty() {
        assertThat(ArchiveStatus.fromCode(null)).isEqualTo(ArchiveStatus.DRAFT);
        assertThat(ArchiveStatus.fromCode("")).isEqualTo(ArchiveStatus.DRAFT);
        assertThat(ArchiveStatus.fromCode("   ")).isEqualTo(ArchiveStatus.DRAFT);
    }

    @Test
    @DisplayName("无效状态代码应该抛出异常")
    void shouldThrowExceptionForInvalidCode() {
        assertThatThrownBy(() -> ArchiveStatus.fromCode("invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid archive status");
    }

    @Test
    @DisplayName("应该正确判断状态是否可转换")
    void shouldValidateStateTransitions() {
        // DRAFT -> PENDING: 允许
        assertThat(ArchiveStatus.DRAFT.canTransitionTo(ArchiveStatus.PENDING)).isTrue();

        // DRAFT -> ARCHIVED: 不允许（需先经过 PENDING）
        assertThat(ArchiveStatus.DRAFT.canTransitionTo(ArchiveStatus.ARCHIVED)).isFalse();

        // PENDING -> ARCHIVED: 允许
        assertThat(ArchiveStatus.PENDING.canTransitionTo(ArchiveStatus.ARCHIVED)).isTrue();

        // PENDING -> DRAFT: 允许（拒绝审核回退）
        assertThat(ArchiveStatus.PENDING.canTransitionTo(ArchiveStatus.DRAFT)).isTrue();

        // ARCHIVED -> 任何状态: 不允许（终态）
        assertThat(ArchiveStatus.ARCHIVED.canTransitionTo(ArchiveStatus.DRAFT)).isFalse();
        assertThat(ArchiveStatus.ARCHIVED.canTransitionTo(ArchiveStatus.PENDING)).isFalse();
        assertThat(ArchiveStatus.ARCHIVED.canTransitionTo(ArchiveStatus.ARCHIVED)).isFalse();
    }

    @Test
    @DisplayName("应该正确判断终态")
    void shouldIdentifyTerminalStates() {
        assertThat(ArchiveStatus.ARCHIVED.isTerminal()).isTrue();
        assertThat(ArchiveStatus.DRAFT.isTerminal()).isFalse();
        assertThat(ArchiveStatus.PENDING.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("JSON 序列化应该输出代码")
    void jsonSerializationShouldOutputCode() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(ArchiveStatus.DRAFT);
        assertThat(json).contains("\"draft\"");
    }

    @Test
    @DisplayName("JSON 反序列化应该支持代码")
    void jsonDeserializationShouldSupportCode() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ArchiveStatus status = mapper.readValue("\"draft\"", ArchiveStatus.class);
        assertThat(status).isEqualTo(ArchiveStatus.DRAFT);
    }

    @Test
    @DisplayName("getCode 应该返回正确的代码值")
    void getCodeShouldReturnCorrectValue() {
        assertThat(ArchiveStatus.DRAFT.getCode()).isEqualTo("draft");
        assertThat(ArchiveStatus.PENDING.getCode()).isEqualTo("pending");
        assertThat(ArchiveStatus.ARCHIVED.getCode()).isEqualTo("archived");
    }

    @Test
    @DisplayName("getDescription 应该返回正确的描述")
    void getDescriptionShouldReturnCorrectValue() {
        assertThat(ArchiveStatus.DRAFT.getDescription()).isEqualTo("草稿");
        assertThat(ArchiveStatus.PENDING.getDescription()).isEqualTo("待审核");
        assertThat(ArchiveStatus.ARCHIVED.getDescription()).isEqualTo("已归档");
    }

    @Test
    @DisplayName("canTransitionTo 应该拒绝 null 目标状态")
    void canTransitionToShouldRejectNullTarget() {
        assertThat(ArchiveStatus.DRAFT.canTransitionTo(null)).isFalse();
        assertThat(ArchiveStatus.PENDING.canTransitionTo(null)).isFalse();
        assertThat(ArchiveStatus.ARCHIVED.canTransitionTo(null)).isFalse();
    }
}
