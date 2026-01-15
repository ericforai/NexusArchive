// Input: Spring Framework, FondsContext
// Output: BatchFondsValidator 类
// Pos: Service Layer

package com.nexusarchive.service.collection;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.security.FondsContext;
import org.springframework.stereotype.Component;

/**
 * 批次全宗校验器
 *
 * 提供统一的全宗权限校验逻辑
 */
@Component
public class BatchFondsValidator {

    /**
     * 校验当前用户是否有权访问指定批次
     *
     * @param batch 要校验的批次
     * @throws BusinessException 如果全宗不匹配
     */
    public void validateFondsAccess(CollectionBatch batch) {
        String currentFonds = FondsContext.requireCurrentFondsNo();
        if (!currentFonds.equals(batch.getFondsCode())) {
            throw new BusinessException(403, "越权操作：非当前全宗数据");
        }
    }

    /**
     * 校验并返回当前全宗号
     *
     * @param requestFondsCode 请求中的全宗号
     * @return 当前全宗号
     * @throws BusinessException 如果全宗不匹配
     */
    public String validateRequestFonds(String requestFondsCode) {
        String currentFonds = FondsContext.requireCurrentFondsNo();
        if (requestFondsCode != null && !currentFonds.equals(requestFondsCode)) {
            throw new BusinessException(403, "越权操作：无法在非当前全宗下创建批次");
        }
        return currentFonds;
    }
}
