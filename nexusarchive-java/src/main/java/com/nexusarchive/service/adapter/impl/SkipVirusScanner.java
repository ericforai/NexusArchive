// Input: Spring Framework、Java 标准库、本地模块
// Output: SkipVirusScanner 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.adapter.impl;

import java.io.InputStream;
import com.nexusarchive.service.adapter.VirusScanAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 跳过病毒扫描适配器
 * 用于生产环境暂时跳过病毒扫描（当没有 ClamAV 时）
 * 警告：这会降低安全性，仅用于临时过渡
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "virus.scan.type", havingValue = "skip")
public class SkipVirusScanner implements VirusScanAdapter {

    @Override
    public boolean scan(InputStream inputStream, String fileName) {
        log.warn("SecurityEvent: Virus scan SKIPPED for file: {} (virus.scan.type=skip)", fileName);
        return true; // 跳过扫描，默认安全
    }
}
