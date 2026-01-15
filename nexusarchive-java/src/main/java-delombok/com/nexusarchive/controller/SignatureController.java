// Input: io.swagger、Lombok、Spring Framework、Spring Security、等
// Output: SignatureController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.signature.SignResult;
import com.nexusarchive.dto.signature.VerifyResult;
import com.nexusarchive.entity.ArcSignatureLog;
import com.nexusarchive.mapper.ArcSignatureLogMapper;
import com.nexusarchive.service.signature.SignatureAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * 电子签章控制器
 *
 * <p>提供电子签章和验签功能，支持国密算法 SM2/SM3</p>
 *
 * 合规要求：
 * - DA/T 94-2022: 电子会计档案管理规范
 * - 信创环境: 支持国密算法 SM2/SM3
 *
 * @author Agent B - 合规开发工程师
 */
@Tag(name = "电子签章", description = """
    电子签章和验签接口。

    **功能说明:**
    - 对数据进行数字签名（SM2/RSA）
    - 验证数据签名有效性
    - 验证 PDF 文件签章
    - 验证 OFD 文件签章
    - 查询签章日志
    - 检查签章服务状态

    **支持算法:**
    - SM2: 国密非对称加密算法（推荐）
    - SM3: 国密哈希算法
    - RSA: 国际通用非对称加密算法

    **签章类型:**
    - 数据签名: 对原始数据签名
    - 文件签章: 对 PDF/OFD 文件签名
    - 批量签章: 批量处理签名请求

    **验签内容:**
    - 签名有效性验证
    - 证书链验证
    - 时间戳验证
    - 签章人信息

    **日志记录:**
    - 签章操作日志
    - 验签结果日志
    - 关联档案/文件信息

    **使用场景:**
    - 档案真实性保证
    - 防篡改验证
    - 审计证据留存

    **权限要求:**
    - SYSTEM_ADMIN: 系统管理员
    - SECURITY_ADMIN: 安全保密员
    """)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/signature")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','SECURITY_ADMIN')")
public class SignatureController {

    @Autowired(required = false)
    private SignatureAdapter signatureAdapter;

    private final ArcSignatureLogMapper signatureLogMapper;

    /**
     * 对数据进行签名
     */
    @PostMapping("/sign")
    @Operation(
        summary = "对数据进行签名",
        description = """
            使用指定证书对数据进行 SM2/RSA 签名。

            **请求参数:**
            - data: 待签名数据（Base64 编码）
            - certAlias: 证书别名
            - archiveId: 关联的档案 ID（可选）
            - fileId: 关联的文件 ID（可选）

            **返回数据 (SignResult):**
            - success: 是否成功
            - signature: 签名值（Base64）
            - certSerialNumber: 证书序列号
            - signerName: 签章人名称
            - signTime: 签章时间
            - algorithm: 签名算法

            **业务规则:**
            - 数据需 Base64 编码
            - 证书必须已导入
            - 签章结果自动记录日志

            **错误处理:**
            - 签章服务不可用: 返回错误提示
            - 证书不存在: 返回错误提示

            **使用场景:**
            - 档案数据签名
            - 元数据签名
            - 审计日志签名
            """,
        operationId = "signData",
        tags = {"电子签章"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "签名成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或签名失败"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "503", description = "签章服务不可用")
    })
    public Result<SignResult> sign(
            @Parameter(description = "待签名数据（Base64编码）", required = true) @RequestParam String data,
            @Parameter(description = "证书别名", required = true) @RequestParam String certAlias,
            @Parameter(description = "关联的档案ID") @RequestParam(required = false) String archiveId,
            @Parameter(description = "关联的文件ID") @RequestParam(required = false) String fileId) {

        if (signatureAdapter == null || !signatureAdapter.isAvailable()) {
            return Result.fail("签章服务不可用，请检查配置");
        }

        try {
            byte[] dataBytes = Base64.getDecoder().decode(data);
            SignResult result = signatureAdapter.sign(dataBytes, certAlias);

            if (result.isSuccess()) {
                // 记录签章日志
                saveSignatureLog(archiveId, fileId, result, null);
                log.info("签章成功: 档案ID={}, 证书别名={}", archiveId, certAlias);
            } else {
                log.warn("签章失败: {}", result.getErrorMessage());
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("签章异常: {}", e.getMessage(), e);
            return Result.fail("签章失败: " + e.getMessage());
        }
    }

    /**
     * 验证签名
     */
    @PostMapping("/verify")
    @Operation(
        summary = "验证签名",
        description = """
            验证数据的数字签名有效性。

            **请求参数:**
            - data: 原始数据（Base64 编码）
            - signature: 签名值（Base64 编码）
            - certAlias: 证书别名
            - archiveId: 关联的档案 ID（可选）
            - fileId: 关联的文件 ID（可选）

            **返回数据 (VerifyResult):**
            - valid: 签名是否有效
            - signerName: 签章人名称
            - certSerialNumber: 证书序列号
            - signTime: 签章时间
            - verifyTime: 验证时间
            - algorithm: 签名算法
            - errorMessage: 错误信息（验证失败时）

            **验证内容:**
            - 签名值验证
            - 证书有效性验证
            - 签章时间验证

            **使用场景:**
            - 档案真实性验证
            - 审计证据验证
            - 数据完整性检查
            """,
        operationId = "verifySignature",
        tags = {"电子签章"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证完成"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "503", description = "签章服务不可用")
    })
    public Result<VerifyResult> verify(
            @Parameter(description = "原始数据（Base64编码）", required = true) @RequestParam String data,
            @Parameter(description = "签名值（Base64编码）", required = true) @RequestParam String signature,
            @Parameter(description = "证书别名", required = true) @RequestParam String certAlias,
            @Parameter(description = "关联的档案ID") @RequestParam(required = false) String archiveId,
            @Parameter(description = "关联的文件ID") @RequestParam(required = false) String fileId) {

        if (signatureAdapter == null || !signatureAdapter.isAvailable()) {
            return Result.fail("签章服务不可用，请检查配置");
        }

        try {
            byte[] dataBytes = Base64.getDecoder().decode(data);
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            VerifyResult result = signatureAdapter.verify(dataBytes, signatureBytes, certAlias);

            // 记录验签日志
            updateSignatureLog(archiveId, fileId, result);

            return Result.success(result);
        } catch (Exception e) {
            log.error("验签异常: {}", e.getMessage(), e);
            return Result.fail("验签失败: " + e.getMessage());
        }
    }

    /**
     * 验证 PDF 文件签章
     */
    @PostMapping("/verify-pdf")
    @Operation(
        summary = "验证 PDF 文件签章",
        description = """
            验证 PDF 文件中的电子签章。

            **请求参数:**
            - file: PDF 文件
            - archiveId: 关联的档案 ID（可选）
            - fileId: 关联的文件 ID（可选）

            **返回数据 (VerifyResult):**
            - valid: 签章是否有效
            - signerName: 签章人名称
            - certSerialNumber: 证书序列号
            - signTime: 签章时间
            - verifyTime: 验证时间
            - errorMessage: 错误信息

            **验证内容:**
            - PDF 内嵌签名验证
            - 签名证书链验证
            - 签章时间戳验证

            **使用场景:**
            - PDF 档案验签
            - 电子发票验证
            - 合同文件验证
            """,
        operationId = "verifyPdfSignature",
        tags = {"电子签章"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证完成"),
        @ApiResponse(responseCode = "400", description = "文件格式错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "503", description = "签章服务不可用")
    })
    public Result<VerifyResult> verifyPdf(
            @Parameter(description = "PDF文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "关联的档案ID") @RequestParam(required = false) String archiveId,
            @Parameter(description = "关联的文件ID") @RequestParam(required = false) String fileId) {

        if (signatureAdapter == null || !signatureAdapter.isAvailable()) {
            return Result.fail("签章服务不可用，请检查配置");
        }

        try {
            InputStream pdfStream = file.getInputStream();
            VerifyResult result = signatureAdapter.verifyPdfSignature(pdfStream);

            // 记录验签日志
            updateSignatureLog(archiveId, fileId, result);

            return Result.success(result);
        } catch (Exception e) {
            log.error("PDF签章验证异常: {}", e.getMessage(), e);
            return Result.fail("PDF签章验证失败: " + e.getMessage());
        }
    }

    /**
     * 验证 OFD 文件签章
     */
    @PostMapping("/verify-ofd")
    @Operation(
        summary = "验证 OFD 文件签章",
        description = """
            验证 OFD 文件中的电子签章。

            **请求参数:**
            - file: OFD 文件
            - archiveId: 关联的档案 ID（可选）
            - fileId: 关联的文件 ID（可选）

            **返回数据 (VerifyResult):**
            - valid: 签章是否有效
            - signerName: 签章人名称
            - certSerialNumber: 证书序列号
            - signTime: 签章时间
            - verifyTime: 验证时间
            - errorMessage: 错误信息

            **OFD 格式:**
            - GB/T 33190-2016《电子文件存储与交换格式 版式文档》
            - 支持国家标准电子签章

            **使用场景:**
            - OFD 档案验签
            - 标准格式文件验证
            - 长期保存文件验证
            """,
        operationId = "verifyOfdSignature",
        tags = {"电子签章"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证完成"),
        @ApiResponse(responseCode = "400", description = "文件格式错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "503", description = "签章服务不可用")
    })
    public Result<VerifyResult> verifyOfd(
            @Parameter(description = "OFD文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "关联的档案ID") @RequestParam(required = false) String archiveId,
            @Parameter(description = "关联的文件ID") @RequestParam(required = false) String fileId) {

        if (signatureAdapter == null || !signatureAdapter.isAvailable()) {
            return Result.fail("签章服务不可用，请检查配置");
        }

        try {
            InputStream ofdStream = file.getInputStream();
            VerifyResult result = signatureAdapter.verifyOfdSignature(ofdStream);

            // 记录验签日志
            updateSignatureLog(archiveId, fileId, result);

            return Result.success(result);
        } catch (Exception e) {
            log.error("OFD签章验证异常: {}", e.getMessage(), e);
            return Result.fail("OFD签章验证失败: " + e.getMessage());
        }
    }

    /**
     * 查询签章日志
     */
    @GetMapping("/logs")
    @Operation(
        summary = "查询签章日志",
        description = """
            根据档案 ID 或文件 ID 查询签章日志。

            **查询参数:**
            - archiveId: 档案 ID（可选）
            - fileId: 文件 ID（可选）

            **返回数据:**
            - 签章日志列表

            **日志内容包括:**
            - 签章人名称
            - 证书序列号
            - 签章时间
            - 签名算法
            - 验证结果
            - 验证时间

            **使用场景:**
            - 签章历史查询
            - 审计证据获取
            - 问题排查
            """,
        operationId = "getSignatureLogs",
        tags = {"电子签章"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<List<ArcSignatureLog>> getSignatureLogs(
            @Parameter(description = "档案ID") @RequestParam(required = false) String archiveId,
            @Parameter(description = "文件ID") @RequestParam(required = false) String fileId) {

        try {
            List<ArcSignatureLog> logs;
            if (archiveId != null) {
                logs = signatureLogMapper.findByArchiveId(archiveId);
            } else if (fileId != null) {
                logs = signatureLogMapper.findByFileId(fileId);
            } else {
                return Result.fail("必须提供 archiveId 或 fileId");
            }
            return Result.success(logs);
        } catch (Exception e) {
            log.error("查询签章日志异常: {}", e.getMessage(), e);
            return Result.fail("查询签章日志失败: " + e.getMessage());
        }
    }

    /**
     * 检查签章服务状态
     */
    @GetMapping("/status")
    @Operation(
        summary = "检查签章服务状态",
        description = """
            检查签章服务是否可用。

            **返回数据 (ServiceStatus):**
            - available: 服务是否可用
            - serviceType: 服务类型（SM2/RSA/NONE）
            - message: 状态消息

            **使用场景:**
            - 服务健康检查
            - 配置验证
            - 故障排查
            """,
        operationId = "getServiceStatus",
        tags = {"电子签章"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<ServiceStatus> getServiceStatus() {
        boolean available = signatureAdapter != null && signatureAdapter.isAvailable();
        String serviceType = available ? signatureAdapter.getServiceType() : "NONE";

        ServiceStatus status = new ServiceStatus();
        status.setAvailable(available);
        status.setServiceType(serviceType);
        status.setMessage(available ? "签章服务可用" : "签章服务不可用，请检查配置");

        return Result.success(status);
    }

    /**
     * 保存签章日志
     */
    private void saveSignatureLog(String archiveId, String fileId, SignResult result, VerifyResult verifyResult) {
        try {
            ArcSignatureLog log = new ArcSignatureLog();
            log.setArchiveId(archiveId);
            log.setFileId(fileId);
            log.setSignerName(result.getSignerName());
            log.setSignerCertSn(result.getCertSerialNumber());
            log.setSignTime(result.getSignTime());
            log.setSignAlgorithm(result.getAlgorithm());
            log.setSignatureValue(Base64.getEncoder().encodeToString(result.getSignature()));

            if (verifyResult != null) {
                log.setVerifyResult(verifyResult.isValid() ? "VALID" : "INVALID");
                log.setVerifyTime(verifyResult.getVerifyTime());
                log.setVerifyMessage(verifyResult.getErrorMessage());
            }

            signatureLogMapper.insert(log);
        } catch (Exception e) {
            log.error("保存签章日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新签章日志（验签结果）
     */
    private void updateSignatureLog(String archiveId, String fileId, VerifyResult result) {
        try {
            List<ArcSignatureLog> logs;
            if (archiveId != null) {
                logs = signatureLogMapper.findByArchiveId(archiveId);
            } else if (fileId != null) {
                logs = signatureLogMapper.findByFileId(fileId);
            } else {
                return;
            }

            if (logs != null && !logs.isEmpty()) {
                ArcSignatureLog log = logs.get(0); // 更新最新的签章日志
                log.setVerifyResult(result.isValid() ? "VALID" : "INVALID");
                log.setVerifyTime(result.getVerifyTime());
                log.setVerifyMessage(result.getErrorMessage());
                signatureLogMapper.updateById(log);
            } else {
                // 如果没有签章日志，创建新的验签日志
                ArcSignatureLog newLog = new ArcSignatureLog();
                newLog.setArchiveId(archiveId);
                newLog.setFileId(fileId);
                newLog.setSignerName(result.getSignerName());
                newLog.setSignerCertSn(result.getCertSerialNumber());
                newLog.setVerifyResult(result.isValid() ? "VALID" : "INVALID");
                newLog.setVerifyTime(result.getVerifyTime());
                newLog.setVerifyMessage(result.getErrorMessage());
                newLog.setSignAlgorithm(result.getAlgorithm());
                signatureLogMapper.insert(newLog);
            }
        } catch (Exception e) {
            log.error("更新签章日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 服务状态 DTO
     */
    public static class ServiceStatus {
        private boolean available;
        private String serviceType;
        private String message;

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public String getServiceType() {
            return serviceType;
        }

        public void setServiceType(String serviceType) {
            this.serviceType = serviceType;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
