// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: SystemSettingService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.entity.SystemSetting;
import com.nexusarchive.mapper.SystemSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingMapper settingMapper;

    public List<SystemSetting> listAll() {
        return settingMapper.selectList(new LambdaQueryWrapper<SystemSetting>()
                .eq(SystemSetting::getDeleted, 0)
                .orderByAsc(SystemSetting::getCategory, SystemSetting::getConfigKey));
    }

    @Transactional
    public void saveAll(List<SystemSetting> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_CONFIG_NOT_FOUND);
        }
        List<String> keys = items.stream().map(SystemSetting::getConfigKey).filter(StringUtils::hasText).collect(Collectors.toList());
        if (keys.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_CONFIG_KEY_CANNOT_BE_EMPTY);
        }
        Map<String, SystemSetting> existingMap = settingMapper.selectList(new LambdaQueryWrapper<SystemSetting>()
                        .eq(SystemSetting::getDeleted, 0)
                        .in(SystemSetting::getConfigKey, keys))
                .stream()
                .collect(Collectors.toMap(SystemSetting::getConfigKey, x -> x, (a, b) -> a));

        for (SystemSetting item : items) {
            if (!StringUtils.hasText(item.getConfigKey())) {
                continue;
            }
            SystemSetting exist = existingMap.get(item.getConfigKey());
            if (exist == null) {
                settingMapper.insert(item);
            } else {
                exist.setConfigValue(item.getConfigValue());
                exist.setDescription(item.getDescription());
                exist.setCategory(item.getCategory());
                settingMapper.updateById(exist);
            }
        }
    }

    /**
     * 初始化默认配置（只在空表时调用）
     */
    @Transactional
    public void initDefaultsIfEmpty() {
        Long count = settingMapper.selectCount(new LambdaQueryWrapper<>());
        if (count != null && count > 0) return;

        List<SystemSetting> defaults = new ArrayList<>();
        defaults.add(build("system.name", "Nexus Archive System", "系统名称", "system"));
        defaults.add(build("archive.prefix", "QZ-2024-", "档号前缀", "archive"));
        defaults.add(build("storage.type", "local", "存储类型 local/nas/oss", "storage"));
        defaults.add(build("storage.path", "/data/archive", "本地存储路径", "storage"));
        defaults.add(build("retention.default", "10Y", "默认保管期限", "archive"));
        settingMapper.insert(defaults.get(0));
        settingMapper.insert(defaults.get(1));
        settingMapper.insert(defaults.get(2));
        settingMapper.insert(defaults.get(3));
        settingMapper.insert(defaults.get(4));
    }

    private SystemSetting build(String key, String value, String desc, String category) {
        SystemSetting s = new SystemSetting();
        s.setConfigKey(key);
        s.setConfigValue(value);
        s.setDescription(desc);
        s.setCategory(category);
        return s;
    }
}
