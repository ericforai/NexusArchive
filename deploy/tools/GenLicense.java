import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class GenLicense {
    private static final String PRIVATE_KEY_B64 = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDEysjhFgFy6SGpugVQpmUgwgA2A/K3kcrqbgMrpXlu3oJVI8PZXW8oFDd2liji8FkoCTrM6/dTKrZ8Q+KMoWjNDAGj9g27TktXpske7r3qU9p68r1jTgauuSPag4gfxoGow1+i2sEA8WyjYhtWWegSJLorwBiBDUat+8higSl96oMXeuG/DWBq5ZV7PefEdsZS+e9HGKTvs82gGee47timBBMrpfF3G2xXURifT8ZIvUR3Bp2B3HAkfChAvi8jTuU2Rsj90GIX1FFqZNfy77uQUO7DQnR7qeUP0L29wxf97oecIRMr0AhvVrGqLVyMTUD7nEnTs42hVijjFM9sVUI5AgMBAAECggEAJZWHFPnzb+NXgwdcgYEt+joi+S362pVVGD3HjDvvu71yTdllS6u9EX3+876M836swGk22jFMzIzxSj1a1Ln0/ohG/e39xGKZIBXh/m7hRZIbPXUDt6YMv6zrTI3nXw418CnUJ5G437m8Il4X03+bQ2/RQI7Q/0CuWYcnR6zcZEgiX8lm9zsYguHgAC3onkF4TeR1r/A2cX2zNd/92LEovXzf3RmsZKBVxTYZpJ8B2LsnvSqb5gHTPCYBLMJHfjHiBd4PDG6kPJNi3sK7WPswaz1FWlO3sdJp40f/yl04prXfjEtiVnk0mdIsKQ5uBJb6pRIzqWKo57jw/svn2Z3PCwKBgQDjy5iYZWe2HfOKbANZ5aa8U5RnTzrKtgSNoQlgXj9zScHsAbqG1z9YLtz5SK9q8olNfm2oj3eympMhwnIp79rrJFnjWFZCjbq5w8PhBy3AE18WHa4KwyReVOK7XXeOp8jQxgkdY2XPEogdUqXFraR5AulQ464ZvrLRp3Lj6uffpwKBgQDdKHv+8x1u3VX1YNFOovSECIYCRN/MQxHPEuO+CrQcmyYn5ovxKH3FpfIV0B/XTCkYNfrlCtHvG2sRtNiWsaaNetubEy7c6Mt7xTwOtBLdpgqH+mg5TCjMXmHX6IWkpIXRTI8LhGCMXDqKf9SX5vk3zrZVCSGJ93uYmykHi9MLHwKBgQC3so3CB7SVBp5JOEWTj0Dgij4Y+amdox8U35JiJl7gp9A8dEBpUNeCU3hbhyP/CMt0tMnVrqyGWWThSp8p9oUCLvRWA/y7+vNjrt0dcN6SEI0TlqetgiSn9ahKFFu/rMqiobDkBC4ryPP/QZDxUtBZzeE9G/5gQIqPy4s5BUwr7QKBgQDD2+60Qyp13J8byLqVIcvagpHrwINSqrNC6D+5NlF8ZArOm2akFHAOcoCKwN7yFQjv4B5qacKyN14DOF34Vyxhb8S8+agvDBvpaSte+isbEMmS0zkqRUB8fbm5NLvmq623Rk4IrRu0MpnFvp/QR0W/HtprRMLnJL3Y0NDtVe95kQKBgAyJH+FVLdvGaL/xsGd9aY4aLxoPtjxHl5qxY0cZW1gjJ55Ul6qRHelxJksr5kdVHMLb8Ao2/+AGAQNauJdysOiyg8hDutMgmE2A8Dl9Z/jjp3spaaEQaZtgifjh9zb4FgXDLrLhGEcB7xwSPPvQmt+fEeF/o27T8owj0jjJv8X9";

    public static void main(String[] args) throws Exception {
        String expireDate = "2030-12-31";
        int maxUsers = 100;
        int nodeLimit = 50;

        String jsonPayload = "{\"expireAt\":\"" + expireDate + "\",\"maxUsers\":" + maxUsers + ",\"nodeLimit\":" + nodeLimit + "}";
        String payloadB64 = Base64.getEncoder().encodeToString(jsonPayload.getBytes());

        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIVATE_KEY_B64)));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privKey);
        signature.update(jsonPayload.getBytes());
        byte[] sigBytes = signature.sign();
        String sigB64 = Base64.getEncoder().encodeToString(sigBytes);

        String license = "{\"payload\":\"" + payloadB64 + "\",\"sig\":\"" + sigB64 + "\"}";
        System.out.println(license);
    }
}
