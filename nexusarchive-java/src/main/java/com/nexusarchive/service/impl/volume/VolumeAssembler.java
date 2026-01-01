// Input: MyBatis-Plus、Lombok、Spring Framework
// Output: VolumeAssembler 类
// Pos: 案卷服务 - 组卷逻辑层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.volume;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Volume;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.VolumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 案卷组装器
 * <p>
 * 负责按月自动组卷，符合 DA/T 104-2024 第7.4节组卷规范
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VolumeAssembler {

    private final VolumeMapper volumeMapper;
    private final ArchiveMapper archiveMapper;

    /**
     * 按月自动组卷
     * 规范: "业务单据一般按月进行组卷"
     *
     * @param fiscalPeriod 会计期间 (YYYY-MM)
     * @return 创建的案卷
     */
    @Transactional
    public Volume assembleByMonth(String fiscalPeriod) {
        log.info("开始按月组卷: {}", fiscalPeriod);

        // 1. 查询该期间内未组卷的凭证
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getFiscalPeriod, fiscalPeriod)
               .eq(Archive::getCategoryCode, "AC01") // 会计凭证
               .isNull(Archive::getVolumeId)
               .eq(Archive::getDeleted, 0);

        List<Archive> archives = archiveMapper.selectList(wrapper);

        if (archives.isEmpty()) {
            log.info("该期间无待组卷凭证: {}", fiscalPeriod);
            throw new BusinessException("该期间没有待组卷的凭证");
        }

        // 2. 获取全宗号和组织名称
        String fondsNo = archives.get(0).getFondsNo();
        String orgName = archives.get(0).getOrgName();
        String fiscalYear = fiscalPeriod.substring(0, 4);
        String month = fiscalPeriod.substring(5, 7);

        // 3. 生成案卷号 (格式: 全宗号-分类号-年月序号)
        String volumeCode = VolumeUtils.generateVolumeCode(fondsNo, "AC01", fiscalPeriod);

        // 4. 生成案卷标题 (格式: 责任者+年度+月度+业务单据名称)
        String title = String.format("%s%s年%s月会计凭证",
                orgName != null ? orgName : fondsNo, fiscalYear, month);

        // 5. 计算保管期限 (取最长)
        String retentionPeriod = VolumeUtils.calculateMaxRetention(archives);

        // 6. 创建案卷
        Volume volume = new Volume();
        volume.setId(UUID.randomUUID().toString().replace("-", ""));
        volume.setVolumeCode(volumeCode);
        volume.setTitle(title);
        volume.setFondsNo(fondsNo);
        volume.setFiscalYear(fiscalYear);
        volume.setFiscalPeriod(fiscalPeriod);
        volume.setCategoryCode("AC01");
        volume.setFileCount(archives.size());
        volume.setRetentionPeriod(retentionPeriod);
        volume.setStatus("draft");
        volume.setCreatedTime(LocalDateTime.now());
        volume.setLastModifiedTime(LocalDateTime.now());

        volumeMapper.insert(volume);
        log.info("案卷创建成功: {} - {}", volumeCode, title);

        // 7. 更新凭证的案卷关联
        for (Archive archive : archives) {
            archive.setVolumeId(volume.getId());
            archive.setLastModifiedTime(LocalDateTime.now());
            archiveMapper.updateById(archive);
        }

        log.info("组卷完成: 案卷={}, 凭证数={}", volumeCode, archives.size());
        return volume;
    }
}
