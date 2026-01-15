// Input: MyBatis-Plus、Lombok、Spring Framework
// Output: VolumeQuery 类
// Pos: 案卷服务 - 查询操作层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.volume;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Volume;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.VolumeMapper;
import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 案卷查询器
 * <p>
 * 负责案卷和卷内文件的查询操作
 * </p>
 */
@UtilityClass
public class VolumeQuery {

    /**
     * 获取案卷列表
     */
    public Page<Volume> getVolumeList(VolumeMapper volumeMapper, int page, int limit, String status) {
        Page<Volume> pageObj = new Page<>(page, limit);
        LambdaQueryWrapper<Volume> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(Volume::getStatus, status);
        }

        wrapper.orderByDesc(Volume::getCreatedTime);
        return volumeMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 获取案卷详情
     */
    public Volume getVolumeById(VolumeMapper volumeMapper, String volumeId) {
        return volumeMapper.selectById(volumeId);
    }

    /**
     * 获取卷内文件列表
     * 规范: "卷内文件一般按照形成时间顺序排列"
     */
    public List<Archive> getVolumeFiles(ArchiveMapper archiveMapper, String volumeId) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getVolumeId, volumeId)
               .eq(Archive::getDeleted, 0)
               .orderByAsc(Archive::getDocDate)   // 按凭证日期
               .orderByAsc(Archive::getArchiveCode); // 按档号
        return archiveMapper.selectList(wrapper);
    }

    /**
     * 生成归档登记表数据
     * 参照 GB/T 18894-2016 附录 A 的表 A.1
     */
    public Map<String, Object> generateRegistrationForm(VolumeMapper volumeMapper, ArchiveMapper archiveMapper, String volumeId) {
        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new RuntimeException("案卷不存在");
        }

        List<Archive> files = getVolumeFiles(archiveMapper, volumeId);

        Map<String, Object> form = new LinkedHashMap<>();
        form.put("registrationNo", "GD-" + volume.getFiscalPeriod() + "-" + System.currentTimeMillis() % 10000);
        form.put("volumeCode", volume.getVolumeCode());
        form.put("volumeTitle", volume.getTitle());
        form.put("fondsNo", volume.getFondsNo());
        form.put("fiscalYear", volume.getFiscalYear());
        form.put("fiscalPeriod", volume.getFiscalPeriod());
        form.put("categoryCode", volume.getCategoryCode());
        form.put("categoryName", "会计凭证");
        form.put("fileCount", volume.getFileCount());
        form.put("retentionPeriod", volume.getRetentionPeriod());
        form.put("registrationTime", java.time.LocalDateTime.now().toString());
        form.put("status", volume.getStatus());

        // 卷内文件清单
        List<Map<String, Object>> fileList = files.stream().map(f -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("序号", files.indexOf(f) + 1);
            item.put("档号", f.getArchiveCode());
            item.put("题名", f.getTitle());
            item.put("日期", f.getDocDate() != null ? f.getDocDate().toString() : "");
            item.put("金额", f.getAmount());
            item.put("制单人", f.getCreator());
            item.put("保管期限", f.getRetentionPeriod());
            return item;
        }).collect(Collectors.toList());

        form.put("fileList", fileList);

        return form;
    }
}
