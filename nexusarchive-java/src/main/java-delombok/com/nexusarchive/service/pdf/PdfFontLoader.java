// Input: Apache PDFBox、Java 标准库
// Output: PdfFontLoader 工具类
// Pos: PDF 字体加载工具层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;

/**
 * PDF 中文字体加载器
 * <p>
 * 跨平台字体加载，支持 macOS、Linux、Docker、Windows
 * </p>
 */
@Slf4j
public class PdfFontLoader {

    private static final String[] FONT_PATHS = {
            // macOS
            "/System/Library/Fonts/STHeiti Light.ttc",
            "/System/Library/Fonts/STHeiti Medium.ttc",
            "/Library/Fonts/Arial Unicode.ttf",
            "/System/Library/Fonts/PingFang.ttc",
            // Linux
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/noto-cjk/NotoSansSC-Regular.otf",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            // Docker Container
            "/usr/share/fonts/truetype/noto/NotoSansSC-Regular.ttf",
            // Windows
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/simhei.ttf"
    };

    /**
     * 加载中文字体
     *
     * @param document PDF 文档
     * @return 中文字体，如果加载失败返回 null
     */
    public static org.apache.pdfbox.pdmodel.font.PDFont loadChineseFont(PDDocument document) {
        for (String fontPath : FONT_PATHS) {
            try {
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    log.debug("Loading Chinese font from: {}", fontPath);
                    return PDType0Font.load(document, fontFile);
                }
            } catch (Exception e) {
                log.trace("Failed to load font from: {}", fontPath);
            }
        }
        log.warn("No Chinese font found, falling back to default fonts");
        return null;
    }

    /**
     * 检查是否有可用中文字体
     *
     * @return 是否有中文字体可用
     */
    public static boolean hasChineseFont() {
        for (String fontPath : FONT_PATHS) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                return true;
            }
        }
        return false;
    }
}
