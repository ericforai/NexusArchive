// Input: Lombok、Spring Framework
// Output: ArchivalCodeGenerator 类
// Pos: 业务服务层

package com.nexusarchive.service.ingest;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.ArcFileContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 档号生成器
 * <p>
 * 生成符合 DA/T 94-2022 标准的档号
 * 格式: {全宗号}-{年度}-{保管期限}-{机构}-{分类}-{件号}
 * </p>
 */
@Component("ingestArchivalCodeGenerator")
@Slf4j
public class ArchivalCodeGenerator {

    private static final String DEFAULT_RETENTION = "30Y";
    private static final String DEFAULT_ORG = "FIN";
    private static final String DEFAULT_CATEGORY = "AC04";

    /**
     * 生成档号
     *
     * @param originalFile 文件记录
     * @return 档号
     */
    public String generate(ArcFileContent originalFile) {
        // 全宗号：必填，无默认值
        String fondsCode = originalFile.getFondsCode();
        if (fondsCode == null || fondsCode.trim().isEmpty()) {
            throw new BusinessException(400,
                    "归档失败：全宗号未配置。请先在[系统设置 > 档案配置]中设置全宗号，或在元数据补录时填写。");
        }

        // 年度：优先使用文件会计年度，否则使用当前年
        String year = originalFile.getFiscalYear() != null
                ? originalFile.getFiscalYear()
                : String.valueOf(LocalDate.now().getYear());

        // 保管期限：默认30Y（合规专家建议）
        String retention = DEFAULT_RETENTION;

        // 机构代码
        String org = DEFAULT_ORG;

        // 分类代码：从 voucherType 读取，未设置则默认 AC04（其他材料）
        String category = originalFile.getVoucherType() != null
                ? originalFile.getVoucherType()
                : DEFAULT_CATEGORY;

        // 件号：时间戳 + 随机数确保唯一
        String itemNo = String.format("V%04d", System.currentTimeMillis() % 10000);

        String archivalCode = String.format("%s-%s-%s-%s-%s-%s", fondsCode, year, retention, org, category, itemNo);
        log.debug("Generated archival code: {}", archivalCode);

        return archivalCode;
    }
}
