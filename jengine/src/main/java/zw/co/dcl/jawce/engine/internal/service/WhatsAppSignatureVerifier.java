package zw.co.dcl.jawce.engine.internal.service;

import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
public class WhatsAppSignatureVerifier {
    public boolean isValid(String rawBody, String appSecret, String xHubSignatureHeader) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            byte[] digest = mac.doFinal(rawBody.getBytes());

            var expectedSignature = "sha256=" + Hex.encodeHexString(digest);

            return expectedSignature.equalsIgnoreCase(xHubSignatureHeader);
        } catch (Exception e) {
            return false;
        }
    }
}
