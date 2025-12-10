
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 这是一个独立的 License 生成工具。
 * 您可以运行此类的 main 方法来：
 * 1. 生成新的 RSA 公钥/私钥对（用于 application.yml 配置）。
 * 2. 使用私钥签发一个新的 License 字符串（给客户）。
 *
 * 依赖库：jackson-databind (在项目中已存在)
 */
public class LicenseGenerator {

    // 替换为您的私钥 (Base64) - 如果您想签发 License，必须填入对应的私钥
    // 第一次运行生成密钥对后，请保存好私钥，并将公钥配置到 application.yml
    private static final String PRIVATE_KEY_B64 = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDEysjhFgFy6SGpugVQpmUgwgA2A/K3kcrqbgMrpXlu3oJVI8PZXW8oFDd2liji8FkoCTrM6/dTKrZ8Q+KMoWjNDAGj9g27TktXpske7r3qU9p68r1jTgauuSPag4gfxoGow1+i2sEA8WyjYhtWWegSJLorwBiBDUat+8higSl96oMXeuG/DWBq5ZV7PefEdsZS+e9HGKTvs82gGee47timBBMrpfF3G2xXURifT8ZIvUR3Bp2B3HAkfChAvi8jTuU2Rsj90GIX1FFqZNfy77uQUO7DQnR7qeUP0L29wxf97oecIRMr0AhvVrGqLVyMTUD7nEnTs42hVijjFM9sVUI5AgMBAAECggEAJZWHFPnzb+NXgwdcgYEt+joi+S362pVVGD3HjDvvu71yTdllS6u9EX3+876M836swGk22jFMzIzxSj1a1Ln0/ohG/e39xGKZIBXh/m7hRZIbPXUDt6YMv6zrTI3nXw418CnUJ5G437m8Il4X03+bQ2/RQI7Q/0CuWYcnR6zcZEgiX8lm9zsYguHgAC3onkF4TeR1r/A2cX2zNd/92LEovXzf3RmsZKBVxTYZpJ8B2LsnvSqb5gHTPCYBLMJHfjHiBd4PDG6kPJNi3sK7WPswaz1FWlO3sdJp40f/yl04prXfjEtiVnk0mdIsKQ5uBJb6pRIzqWKo57jw/svn2Z3PCwKBgQDjy5iYZWe2HfOKbANZ5aa8U5RnTzrKtgSNoQlgXj9zScHsAbqG1z9YLtz5SK9q8olNfm2oj3eympMhwnIp79rrJFnjWFZCjbq5w8PhBy3AE18WHa4KwyReVOK7XXeOp8jQxgkdY2XPEogdUqXFraR5AulQ464ZvrLRp3Lj6uffpwKBgQDdKHv+8x1u3VX1YNFOovSECIYCRN/MQxHPEuO+CrQcmyYn5ovxKH3FpfIV0B/XTCkYNfrlCtHvG2sRtNiWsaaNetubEy7c6Mt7xTwOtBLdpgqH+mg5TCjMXmHX6IWkpIXRTI8LhGCMXDqKf9SX5vk3zrZVCSGJ93uYmykHi9MLHwKBgQC3so3CB7SVBp5JOEWTj0Dgij4Y+amdox8U35JiJl7gp9A8dEBpUNeCU3hbhyP/CMt0tMnVrqyGWWThSp8p9oUCLvRWA/y7+vNjrt0dcN6SEI0TlqetgiSn9ahKFFu/rMqiobDkBC4ryPP/QZDxUtBZzeE9G/5gQIqPy4s5BUwr7QKBgQDD2+60Qyp13J8byLqVIcvagpHrwINSqrNC6D+5NlF8ZArOm2akFHAOcoCKwN7yFQjv4B5qacKyN14DOF34Vyxhb8S8+agvDBvpaSte+isbEMmS0zkqRUB8fbm5NLvmq623Rk4IrRu0MpnFvp/QR0W/HtprRMLnJL3Y0NDtVe95kQKBgAyJH+FVLdvGaL/xsGd9aY4aLxoPtjxHl5qxY0cZW1gjJ55Ul6qRHelxJksr5kdVHMLb8Ao2/+AGAQNauJdysOiyg8hDutMgmE2A8Dl9Z/jjp3spaaEQaZtgifjh9zb4FgXDLrLhGEcB7xwSPPvQmt+fEeF/o27T8owj0jjJv8X9";

    public static void main(String[] args) throws Exception {
        // 模式 1: 生成新的密钥对 (首次使用时运行)
        // generateKeyPair();

        // 模式 2: 签发 License (给客户授权时运行，需先填入上面的 PRIVATE_KEY_B64)
        signLicense("2030-12-31", 100, 50);
    }

    private static void generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        String pubKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String privKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

        System.out.println("========== 新生成的密钥对 ==========");
        System.out.println("[Public Key] (请配置到 application.yml: license.public-key):");
        System.out.println(pubKey);
        System.out.println("\n[Private Key] (请严格保密，用于签发 License，填入 LicenseGenerator 代码中):");
        System.out.println(privKey);
        System.out.println("==================================");
    }

    private static void signLicense(String expireDate, int maxUsers, int nodeLimit) throws Exception {
        if ("YOUR_PRIVATE_KEY_HERE".equals(PRIVATE_KEY_B64)) {
            System.err.println("错误: 请先在代码中填入 PRIVATE_KEY_B64");
            return;
        }

        // 1. 构造 Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("expireAt", expireDate);
        payload.put("maxUsers", maxUsers);
        payload.put("nodeLimit", nodeLimit);
        
        ObjectMapper mapper = new ObjectMapper();
        String jsonPayload = mapper.writeValueAsString(payload);
        String payloadB64 = Base64.getEncoder().encodeToString(jsonPayload.getBytes());

        // 2. 签名
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIVATE_KEY_B64)));
        
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privKey);
        signature.update(jsonPayload.getBytes());
        byte[] sigBytes = signature.sign();
        String sigB64 = Base64.getEncoder().encodeToString(sigBytes);

        // 3. 构造最终 License
        Map<String, String> finalLicense = new HashMap<>();
        finalLicense.put("payload", payloadB64);
        finalLicense.put("sig", sigB64);

        System.out.println("========== LICENSE 生成成功 ==========");
        System.out.println("有效期至: " + expireDate);
        System.out.println("最大用户: " + maxUsers);
        System.out.println("\n请将以下 JSON 字符串发送给客户 (可在系统管理的 'License管理' 处导入):");
        System.out.println(mapper.writeValueAsString(finalLicense));
        System.out.println("=====================================");
    }
}
