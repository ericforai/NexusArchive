// Input: 检索条件与 Mapper
// Output: 搜索结果
// Pos: NexusCore search
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.core.domain.ArchiveObject;
import com.nexusarchive.core.mapper.ArchiveObjectMapper;
import com.nexusarchive.core.masking.Masked;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 默认高级检索服务实现
 */
@Service
@RequiredArgsConstructor
public class DefaultArchiveSearchService implements ArchiveSearchService {
    
    private final ArchiveObjectMapper archiveObjectMapper;

    @Override
    @Masked
    public IPage<ArchiveObject> search(ArchiveSearchRequest request, Page<ArchiveObject> pageable) {
        LambdaQueryWrapper<ArchiveObject> wrapper = new LambdaQueryWrapper<>();
        
        // 1. 基础隔离条件 (全宗/年度)
        // 注意：FondsIsolationInterceptor 可能还会再次强制注入，这里显式添加作为业务条件
        if (StringUtils.hasText(request.getFondsNo())) {
            wrapper.eq(ArchiveObject::getFondsNo, request.getFondsNo());
        }
        if (request.getArchiveYear() != null) {
            wrapper.eq(ArchiveObject::getArchiveYear, String.valueOf(request.getArchiveYear()));
        }
        
        // 2. 结构化范围查询 (利用 BTree 索引)
        if (request.getAmountFrom() != null) {
            wrapper.ge(ArchiveObject::getAmount, request.getAmountFrom());
        }
        if (request.getAmountTo() != null) {
            wrapper.le(ArchiveObject::getAmount, request.getAmountTo());
        }
        
        if (request.getDateFrom() != null) {
            wrapper.ge(ArchiveObject::getDocDate, request.getDateFrom());
        }
        if (request.getDateTo() != null) {
            wrapper.le(ArchiveObject::getDocDate, request.getDateTo());
        }
        
        // 3. 精确/模糊匹配
        if (StringUtils.hasText(request.getCounterparty())) {
            wrapper.like(ArchiveObject::getCounterparty, request.getCounterparty());
        }
        
        if (StringUtils.hasText(request.getVoucherNo())) {
            wrapper.eq(ArchiveObject::getVoucherNo, request.getVoucherNo());
        }
        
        if (StringUtils.hasText(request.getInvoiceNo())) {
            wrapper.eq(ArchiveObject::getInvoiceNo, request.getInvoiceNo());
        }
        
        // 4. 全文检索 (兼容 keyword 搜索 title 或 invoiceNo)
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(ArchiveObject::getTitle, request.getKeyword())
                    .or()
                    .eq(ArchiveObject::getInvoiceNo, request.getKeyword()));
        }
        
        // 5. 默认排序 (按日期倒序)
        wrapper.orderByDesc(ArchiveObject::getDocDate);
        
        return archiveObjectMapper.selectPage(pageable, wrapper);
    }
}
