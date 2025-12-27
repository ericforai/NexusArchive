// Input: Java Crypto API
// Output: LicenseGenerator class
// Pos: Tools Layer - Security Implementation
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 独立的 License 生成工具
 * 
 * 用法:
 * 1. 编译: javac LicenseGenerator.java
 * 2. 运行: java LicenseGenerator
 * 
 * 功能:
 * 1. 生成新的 RSA 公私钥对 (可选)
 * 2. 或者加载已有私钥 (TODO: 暂时每次生成新的，因为没有地方存私钥)
 * 3. 输入授权信息 (过期时间、用户数)
 * 4. 生成签名并输出 license.json
 */
public class LicenseGenerator {

    private static final String LICENSE_FILENAME = "license.json";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("==========================================");
        System.out.println("   NexusArchive License 生成工具");
        System.out.println("==========================================");
        System.out.println();

        try {
            // 1. 生成密钥对
            System.out.println("[1/4] 生成 RSA 密钥对...");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());

            System.out.println("✅ 密钥生成成功!");
            System.out.println();
            System.out.println("⚠️  请务必将以下公钥配置到 application.yml (license.public-key):");
            System.out.println("----------------------------------------------------------------");
            System.out.println(publicKeyBase64);
            System.out.println("----------------------------------------------------------------");
            System.out.println();

            // 2. 收集 License 信息
            System.out.println("[2/4] 配置授权信息");
            
            // 过期时间
            LocalDate expireDate = null;
            while (expireDate == null) {
                System.out.print("请输入过期时间 (格式 YYYY-MM-DD, 例如 2099-12-31): ");
                String input = scanner.nextLine().trim();
                try {
                    expireDate = LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE);
                    if (expireDate.isBefore(LocalDate.now())) {
                        System.out.println("❌ 错误: 日期必须在今天之后");
                        expireDate = null;
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("❌ 错误: 日期格式无效");
                }
            }

            // 最大用户数
            Integer maxUsers = null;
            while (maxUsers == null) {
                System.out.print("请输入最大用户数 (默认 999): ");
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    maxUsers = 999;
                } else {
                    try {
                        maxUsers = Integer.parseInt(input);
                        if (maxUsers <= 0) {
                            System.out.println("❌ 错误: 用户数必须大于 0");
                            maxUsers = null;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("❌ 错误: 请输入有效的数字");
                    }
                }
            }

             // 节点数
            Integer nodeLimit = null;
             while (nodeLimit == null) {
                System.out.print("请输入最大节点数 (默认 10): ");
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    nodeLimit = 10;
                } else {
                    try {
                        nodeLimit = Integer.parseInt(input);
                        if (nodeLimit <= 0) {
                             System.out.println("❌ 错误: 节点数必须大于 0");
                            nodeLimit = null;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("❌ 错误: 请输入有效的数字");
                    }
                }
            }

            // 3. 构造 Payload 并签名
            System.out.println();
            System.out.println("[3/4] 生成签名...");
            
            // 简单的 JSON 拼接，避免依赖 Jackson (保持工具独立性)
            String payloadJson = String.format(
                "{\"expireAt\":\"%s\",\"maxUsers\":%d,\"nodeLimit\":%d}",
                expireDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                maxUsers,
                nodeLimit
            );
            
            // Base64 Payload
            String payloadBase64 = Base64.getEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            // 签名
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payloadJson.getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = signature.sign();
            String sigBase64 = Base64.getEncoder().encodeToString(sigBytes);

            // 最终 License JSON
            String licenseFileContent = String.format(
                "{\n  \"payload\": \"%s\",\n  \"sig\": \"%s\"\n}",
                payloadBase64,
                sigBase64
            );

            // 4. 保存文件
            System.out.println("[4/4] 保存文件...");
            Path outputPath = Paths.get(LICENSE_FILENAME);
            Files.writeString(outputPath, licenseFileContent);

            System.out.println();
            System.out.println("🎉 成功! License 已保存到: " + outputPath.toAbsolutePath());
            System.out.println("有效期: " + expireDate);
            System.out.println("用户数: " + maxUsers);
            System.out.println();
            System.out.println("下一步:");
            System.out.println("1. 将生成的公钥更新到服务器配置中");
            System.out.println("2. 将 license.json 分发给客户或放入服务器 data 目录");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ 发生错误: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}
