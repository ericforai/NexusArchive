// Input: AccountRole enum
// Output: AccountRoleMapper
// Pos: Matching Engine
// 负责将会计科目代码/名称映射到角色

package com.nexusarchive.engine.matching.loader;

import com.nexusarchive.engine.matching.enums.AccountRole;
import org.springframework.stereotype.Component;

/**
 * 科目角色映射器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>根据科目代码前缀映射角色（优先）</li>
 *   <li>根据科目名称关键词映射角色（兜底）</li>
 * </ul>
 */
@Component
public class AccountRoleMapper {

    /**
     * 将科目代码和名称映射到角色
     *
     * @param code 科目代码
     * @param name 科目名称
     * @return 对应的角色，如果无法识别返回 null
     */
    public AccountRole mapSubjectToRole(String code, String name) {
        if (code == null) code = "";
        if (name == null) name = "";

        // 1. 基于编码规则 (优先)
        AccountRole role = mapByCode(code);
        if (role != null) {
            return role;
        }

        // 2. 基于名称关键词 (兜底)
        return mapByName(name);
    }

    /**
     * 根据科目代码映射角色
     */
    private AccountRole mapByCode(String code) {
        return switch (code.substring(0, Math.min(4, code.length()))) {
            case "1001" -> AccountRole.CASH;
            case "1002" -> AccountRole.BANK;
            case "1122", "1123" -> AccountRole.RECEIVABLE;
            case "2202", "2203" -> AccountRole.PAYABLE;
            case "2221" -> AccountRole.TAX;
            case "2211" -> AccountRole.SALARY;
            default -> {
                if (code.startsWith("66") || code.startsWith("5")) yield AccountRole.EXPENSE;
                if (code.startsWith("60") || code.startsWith("63")) yield AccountRole.REVENUE;
                if (code.startsWith("14")) yield AccountRole.INVENTORY;
                if (code.startsWith("16")) yield AccountRole.ASSET;
                yield null;
            }
        };
    }

    /**
     * 根据科目名称映射角色
     */
    private AccountRole mapByName(String name) {
        if (name.contains("银行")) return AccountRole.BANK;
        if (name.contains("现金")) return AccountRole.CASH;
        if (name.contains("薪酬") || name.contains("工资") || name.contains("奖金")) return AccountRole.SALARY;
        if (name.contains("税")) return AccountRole.TAX;
        if (name.contains("应收")) return AccountRole.RECEIVABLE;
        if (name.contains("应付")) return AccountRole.PAYABLE;
        if (name.contains("费") || name.contains("折旧")) return AccountRole.EXPENSE;
        if (name.contains("收入")) return AccountRole.REVENUE;
        return null;
    }
}
