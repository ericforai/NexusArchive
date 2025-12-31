// Input: 水印参数
// Output: 水印配置 DTO
// Pos: NexusCore compliance/watermark
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

/**
 * 水印配置
 */
public record WatermarkConfig(
    String primaryText,    // 用户名
    String secondaryText,  // TraceID + FondsNo
    float opacity,         // 0.1 - 0.3
    float rotation,        // 45 度
    String fontName,       // "SimSun" 或 "STSong"
    float fontSize         // 48
) {
    public static WatermarkConfig of(String primaryText, String secondaryText) {
        return new WatermarkConfig(
                primaryText,
                secondaryText,
                0.15f,
                45f,
                "SimSun",
                48f
        );
    }
}
