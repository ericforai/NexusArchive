package com.nexusarchive.service.converter;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.Img;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * OFD 格式转换助手
 * 负责将 PDF、图片等异构文件统一转换为 OFD 标准格式
 * 
 * 使用 Apache PDFBox 将 PDF 渲染为图片，再用 OFDRW 封装为 OFD
 */
@Slf4j
@Component
public class OfdConverterHelper {

    // PDF 渲染 DPI（越高越清晰，但文件越大）
    private static final float RENDER_DPI = 150f;

    /**
     * 将文件转换为 OFD
     * @param source 源文件路径
     * @param target 目标 OFD 路径
     * @throws IOException 转换失败
     */
    public void convertToOfd(Path source, Path target) throws IOException {
        String fileName = source.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".pdf")) {
            convertPdfToOfd(source, target);
        } else if (isImage(fileName)) {
            convertImageToOfd(source, target);
        } else {
            // 对于不支持的格式（如 Office），目前暂不支持直接转换
            if (fileName.endsWith(".ofd")) {
                Files.copy(source, target);
            } else {
                throw new UnsupportedOperationException("不支持的文件格式转换: " + fileName);
            }
        }
    }

    /**
     * 将 PDF 文件转换为 OFD
     * 原理：使用 PDFBox 将每页渲染为图片，再将图片嵌入 OFD 文档
     */
    private void convertPdfToOfd(Path source, Path target) throws IOException {
        log.info("开始 PDF 转 OFD: {}", source);
        
        List<Path> tempImages = new ArrayList<>();
        
        try (PDDocument pdfDoc = PDDocument.load(source.toFile())) {
            PDFRenderer renderer = new PDFRenderer(pdfDoc);
            int pageCount = pdfDoc.getNumberOfPages();
            log.info("PDF 共 {} 页", pageCount);
            
            // Step 1: 将每页渲染为临时图片
            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, RENDER_DPI, ImageType.RGB);
                
                // 创建临时文件保存图片
                Path tempImage = Files.createTempFile("pdf_page_" + i + "_", ".png");
                ImageIO.write(image, "PNG", tempImage.toFile());
                tempImages.add(tempImage);
                
                log.debug("渲染第 {} 页完成", i + 1);
            }
            
            // Step 2: 将所有图片嵌入 OFD 文档
            try (OFDDoc ofdDoc = new OFDDoc(target)) {
                for (int i = 0; i < tempImages.size(); i++) {
                    Path imagePath = tempImages.get(i);
                    Img img = new Img(imagePath);
                    
                    // 设置图片宽度为页面宽度（A4: 210mm），高度自动计算
                    img.setWidth(210d);
                    
                    ofdDoc.add(img);
                    log.debug("添加第 {} 页到 OFD", i + 1);
                }
            }
            
            log.info("PDF 转 OFD 完成: {} -> {}", source.getFileName(), target.getFileName());
            
        } finally {
            // 清理临时图片文件
            for (Path tempImage : tempImages) {
                try {
                    Files.deleteIfExists(tempImage);
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", tempImage);
                }
            }
        }
    }

    /**
     * 将图片直接转换为 OFD
     */
    private void convertImageToOfd(Path source, Path target) throws IOException {
        log.info("开始图片转 OFD: {}", source);
        
        try (OFDDoc ofdDoc = new OFDDoc(target)) {
            Img img = new Img(source);
            // 自动调整大小适应页面
            img.setWidth(210d); // A4 宽度
            ofdDoc.add(img);
        }
        
        log.info("图片转 OFD 完成: {}", target);
    }

    private boolean isImage(String fileName) {
        return fileName.endsWith(".jpg") || 
               fileName.endsWith(".jpeg") || 
               fileName.endsWith(".png") || 
               fileName.endsWith(".bmp") ||
               fileName.endsWith(".gif") ||
               fileName.endsWith(".tiff") ||
               fileName.endsWith(".tif");
    }
}

