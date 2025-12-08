package com.nexusarchive.service.signature;

import com.nexusarchive.dto.signature.SignResult;
import com.nexusarchive.dto.signature.VerifyResult;

import java.io.InputStream;

/**
 * 电子签章适配器接口
 * 
 * 定义电子签章服务的标准接口，支持多种签章服务商的适配
 * 
 * 合规要求：
 * - DA/T 94-2022: 电子会计档案元数据规范
 * - 信创环境: 必须支持国密算法 SM2/SM3
 * 
 * @author Agent B - 合规开发工程师
 */
public interface SignatureAdapter {
    
    /**
     * 对数据进行签名
     * 
     * @param data 待签名数据
     * @param certAlias 证书别名
     * @return 签名结果
     */
    SignResult sign(byte[] data, String certAlias);
    
    /**
     * 验证签名
     * 
     * @param data 原始数据
     * @param signature 签名值
     * @param certAlias 证书别名
     * @return 验证结果
     */
    VerifyResult verify(byte[] data, byte[] signature, String certAlias);
    
    /**
     * 验证 PDF 文件签章
     * 
     * @param pdfStream PDF 文件输入流
     * @return 验证结果
     */
    VerifyResult verifyPdfSignature(InputStream pdfStream);
    
    /**
     * 验证 OFD 文件签章
     * 
     * @param ofdStream OFD 文件输入流
     * @return 验证结果
     */
    VerifyResult verifyOfdSignature(InputStream ofdStream);
    
    /**
     * 获取签章服务类型
     * 
     * @return 服务类型: SM2, RSA, THIRD_PARTY
     */
    String getServiceType();
    
    /**
     * 检查签章服务是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
}
