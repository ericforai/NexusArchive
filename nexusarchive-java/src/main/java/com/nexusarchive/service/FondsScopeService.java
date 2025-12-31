// Input: Spring Framework、MyBatis-Plus、Java 标准库
// Output: FondsScopeService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.mapper.SysUserFondsScopeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FondsScopeService {

    private final SysUserFondsScopeMapper scopeMapper;

    /**
     * 获取用户可访问的全宗号列表
     */
    public List<String> getAllowedFonds(String userId) {
        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }
        List<String> fonds = scopeMapper.findFondsNoByUserId(userId);
        if (fonds == null || fonds.isEmpty()) {
            return Collections.emptyList();
        }
        return fonds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
