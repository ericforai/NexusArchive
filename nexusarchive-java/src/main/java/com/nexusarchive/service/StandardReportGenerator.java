package com.nexusarchive.service;

import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;

/**
 * 标准报告生成器
 * 符合GB/T 39674和DA/T 94-2022标准
 */
@Slf4j
@Service
public class StandardReportGenerator {
    
    private static final String NAMESPACE_URI = "http://www.saac.gov.cn/national/standard";
    private static final String STANDARD_VERSION = "DA/T 94-2022";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 生成符合国家标准的四性检测报告
     * @param report 四性检测结果
     * @return XML格式的报告字符串
     */
    public String generateComplianceReport(FourNatureReport report) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            // 启用命名空间支持
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            
            // 创建文档根元素
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElementNS(NAMESPACE_URI, "档案四性检测报告");
            
            // 设置默认命名空间前缀
            rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            rootElement.setAttribute("xsi:schemaLocation", 
                NAMESPACE_URI + " " + NAMESPACE_URI.replace("/standard", "/schema") + "/four-nature-report.xsd");
            
            // 添加版本信息
            rootElement.setAttribute("版本", STANDARD_VERSION);
            rootElement.setAttribute("生成时间", report.getCheckTime().format(DATETIME_FORMATTER));
            
            doc.appendChild(rootElement);
            
            // 添加基本信息
            addBasicInfo(doc, rootElement, report);
            
            // 添加检测环境信息
            addEnvironmentInfo(doc, rootElement);
            
            // 添加四性检测结果
            addFourNatureResults(doc, rootElement, report);
            
            // 添加总体结论
            addOverallConclusion(doc, rootElement, report);
            
            // 转换为XML字符串
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            
            return writer.toString();
            
        } catch (Exception e) {
            log.error("生成合规报告失败", e);
            throw new RuntimeException("生成合规报告失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成符合国家标准的四性检测报告（保存到文件）
     * @param report 四性检测结果
     * @param filePath 文件路径
     */
    public void saveComplianceReportToFile(FourNatureReport report, String filePath) {
        try {
            String xmlContent = generateComplianceReport(report);
            
            java.io.FileWriter writer = new java.io.FileWriter(filePath);
            writer.write(xmlContent);
            writer.close();
            
            log.info("合规报告已保存到: {}", filePath);
        } catch (Exception e) {
            log.error("保存合规报告失败", e);
            throw new RuntimeException("保存合规报告失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 添加基本信息
     */
    private void addBasicInfo(Document doc, Element parent, FourNatureReport report) {
        Element basicInfo = doc.createElementNS(NAMESPACE_URI, "基本信息");
        parent.appendChild(basicInfo);
        
        addElement(doc, basicInfo, "检测ID", report.getCheckId());
        addElement(doc, basicInfo, "档号", report.getArchivalCode());
        addElement(doc, basicInfo, "检测时间", report.getCheckTime().format(DATETIME_FORMATTER));
        addElement(doc, basicInfo, "检测日期", report.getCheckTime().format(DATE_FORMATTER));
        addElement(doc, basicInfo, "总体结果", report.getStatus().name());
    }
    
    /**
     * 添加检测环境信息
     */
    private void addEnvironmentInfo(Document doc, Element parent) {
        Element envInfo = doc.createElementNS(NAMESPACE_URI, "检测环境信息");
        parent.appendChild(envInfo);
        
        // 获取系统信息
        addElement(doc, envInfo, "操作系统", System.getProperty("os.name"));
        addElement(doc, envInfo, "操作系统版本", System.getProperty("os.version"));
        addElement(doc, envInfo, "Java版本", System.getProperty("java.version"));
        addElement(doc, envInfo, "应用版本", "2.0.0"); // 可以从配置中获取
        addElement(doc, envInfo, "检测人员", "系统自动");
        addElement(doc, envInfo, "检测标准", STANDARD_VERSION);
        addElement(doc, envInfo, "引用标准", "GB/T 39674-2020 DA/T 94-2022");
    }
    
    /**
     * 添加四性检测结果
     */
    private void addFourNatureResults(Document doc, Element parent, FourNatureReport report) {
        Element fourNature = doc.createElementNS(NAMESPACE_URI, "四性检测结果");
        parent.appendChild(fourNature);
        
        // 真实性
        addNatureResult(doc, fourNature, "真实性", report.getAuthenticity());
        
        // 完整性
        addNatureResult(doc, fourNature, "完整性", report.getIntegrity());
        
        // 可用性
        addNatureResult(doc, fourNature, "可用性", report.getUsability());
        
        // 安全性
        addNatureResult(doc, fourNature, "安全性", report.getSafety());
    }
    
    /**
     * 添加单个性质检测结果
     */
    private void addNatureResult(Document doc, Element parent, String natureName, CheckItem checkItem) {
        Element natureElement = doc.createElementNS(NAMESPACE_URI, natureName);
        parent.appendChild(natureElement);
        
        addElement(doc, natureElement, "结果", checkItem.getStatus().name());
        addElement(doc, natureElement, "描述", checkItem.getMessage() != null ? checkItem.getMessage() : "");
        addElement(doc, natureElement, "检测时间", checkItem.getCheckTime().format(DATETIME_FORMATTER));
        addElement(doc, natureElement, "检测方法", getCheckMethod(natureName));
        
        if (!checkItem.getErrors().isEmpty()) {
            Element errorsElement = doc.createElementNS(NAMESPACE_URI, "错误列表");
            natureElement.appendChild(errorsElement);
            
            for (int i = 0; i < checkItem.getErrors().size(); i++) {
                addElement(doc, errorsElement, "错误项", checkItem.getErrors().get(i));
            }
        }
    }
    
    /**
     * 添加总体结论
     */
    private void addOverallConclusion(Document doc, Element parent, FourNatureReport report) {
        Element conclusion = doc.createElementNS(NAMESPACE_URI, "总体结论");
        parent.appendChild(conclusion);
        
        String conclusionText;
        OverallStatus status = report.getStatus();
        
        switch (status) {
            case PASS:
                conclusionText = "档案四性检测全部通过，符合归档标准";
                break;
            case WARNING:
                conclusionText = "档案四性检测存在警告项，建议修复后重新检测";
                break;
            case FAIL:
                conclusionText = "档案四性检测未通过，存在严重问题，不允许归档";
                break;
            default:
                conclusionText = "档案四性检测结果未知";
                break;
        }
        
        addElement(doc, conclusion, "结论", conclusionText);
        addElement(doc, conclusion, "建议", getRecommendation(report.getStatus()));
    }
    
    /**
     * 获取检测方法
     */
    private String getCheckMethod(String natureName) {
        switch (natureName) {
            case "真实性":
                return "SM3/SHA256哈希值比对";
            case "完整性":
                return "元数据字段与文件关联校验";
            case "可用性":
                return "文件格式检测与解析测试";
            case "安全性":
                return "病毒扫描与安全检测";
            default:
                return "未定义检测方法";
        }
    }
    
    /**
     * 获取处理建议
     */
    private String getRecommendation(OverallStatus status) {
        switch (status) {
            case PASS:
                return "档案符合归档要求，可进入下一流程";
            case WARNING:
                return "修复警告项后重新检测，确保档案长期保存安全";
            case FAIL:
                return "修复所有严重问题后重新提交检测";
            default:
                return "请联系管理员获取支持";
        }
    }
    
    /**
     * 添加元素
     */
    private void addElement(Document doc, Element parent, String name, String value) {
        Element element = doc.createElementNS(NAMESPACE_URI, name);
        if (value != null) {
            element.appendChild(doc.createTextNode(value));
        }
        parent.appendChild(element);
    }
}