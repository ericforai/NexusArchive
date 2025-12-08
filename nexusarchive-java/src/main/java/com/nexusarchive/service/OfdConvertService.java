package com.nexusarchive.service;

import java.util.List;
import java.util.Map;

public interface OfdConvertService {

    /**
     * 将指定档案的 PDF 文件转换为 OFD
     *
     * @param archiveId 档案ID
     * @return 转换结果信息 (Map包含 targetPath 等)
     */
    Map<String, Object> convertToOfd(String archiveId);

    /**
     * 批量转换
     *
     * @param archiveIds 档案ID列表
     * @return 成功转换的数量
     */
    int batchConvert(List<String> archiveIds);
    
    /**
     * 根据 PDF 路径进行转换 (底层方法)
     * 
     * @param sourcePath 源文件绝对路径
     * @param targetPath 目标文件绝对路径
     * @return true 如果转换成功
     */
    boolean convertPdfToOfd(String sourcePath, String targetPath) throws Exception;
}
