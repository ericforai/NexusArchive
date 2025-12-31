// Input: AspectJ, DefaultDataMaskingService
// Output: Masking Aspect
// Pos: NexusCore masking
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.masking;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class MaskingAspect {

    private final DataMaskingService maskingService;

    @AfterReturning(pointcut = "@annotation(com.nexusarchive.core.masking.Masked)", returning = "result")
    public void doMasking(Object result) {
        if (result == null) {
            return;
        }

        if (result instanceof IPage) {
            IPage<?> page = (IPage<?>) result;
            maskingService.maskList(page.getRecords());
        } else if (result instanceof Collection) {
            maskingService.maskList((List<?>) result); // 假设是 List，Set 暂不处理或转 List
        } else {
            maskingService.maskObject(result);
        }
    }
}
