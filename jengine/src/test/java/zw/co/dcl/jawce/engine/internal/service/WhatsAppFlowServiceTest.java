package zw.co.dcl.jawce.engine.internal.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.WhatsAppFlowConstant;
import zw.co.dcl.jawce.engine.model.core.FlowEndpointPayload;
import zw.co.dcl.jawce.engine.model.core.FlowEndpointResponse;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WhatsAppFlowServiceTest {
    @Test
    void decryptPayloadRestoresWhatsappFlowEnvelope() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        WhatsAppFlowService service = serviceWithPrivateKey(keyPair.getPrivate());

        byte[] aesKey = generateAesKey();
        byte[] iv = new byte[12];
        for (int i = 0; i < iv.length; i++) {
            iv[i] = (byte) (i + 1);
        }

        Map<String, Object> plainPayload = Map.of(
                "version", "3",
                "action", "data_exchange",
                "screen", "BOOK_SCREEN",
                "flow_token", "flow-token-123",
                "data", Map.of("name", "TDD User")
        );

        Cipher rsaCipher = Cipher.getInstance(WhatsAppFlowConstant.RSA_OAEP);
        rsaCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey);

        Cipher aesCipher = Cipher.getInstance(WhatsAppFlowConstant.AES_CIPHER);
        aesCipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(aesKey, WhatsAppFlowConstant.AES_ALGO),
                new GCMParameterSpec(WhatsAppFlowConstant.GCM_TAG_LENGTH_BITS, iv)
        );
        byte[] encryptedFlowData = aesCipher.doFinal(SerializeUtils.toByteArray(plainPayload));

        FlowEndpointResponse response = service.decryptPayload(Map.of(
                "encrypted_flow_data", Base64.getEncoder().encodeToString(encryptedFlowData),
                "encrypted_aes_key", Base64.getEncoder().encodeToString(encryptedAesKey),
                "initial_vector", Base64.getEncoder().encodeToString(iv)
        ));

        FlowEndpointPayload payload = response.payload();
        assertEquals("3", payload.getVersion());
        assertEquals("data_exchange", payload.getAction());
        assertEquals("BOOK_SCREEN", payload.getScreen());
        assertEquals("flow-token-123", payload.getFlowToken());
        assertEquals("TDD User", payload.getData().get("name"));
        assertArrayEquals(aesKey, response.aesKey());
        assertArrayEquals(iv, response.iv());
    }

    @Test
    void encryptResponseUsesFlippedIvAndCanBeDecryptedBack() throws Exception {
        WhatsAppFlowService service = serviceWithPrivateKey(generateRsaKeyPair().getPrivate());
        byte[] aesKey = generateAesKey();
        byte[] iv = new byte[12];
        for (int i = 0; i < iv.length; i++) {
            iv[i] = (byte) (10 + i);
        }

        Map<String, Object> responsePayload = Map.of(
                "version", "3",
                "data", Map.of("message", "Thanks, received")
        );

        String encrypted = service.encryptResponse(
                responsePayload,
                new FlowEndpointResponse(new FlowEndpointPayload(), aesKey, iv)
        );

        byte[] flippedIv = new byte[iv.length];
        for (int i = 0; i < iv.length; i++) {
            flippedIv[i] = (byte) (iv[i] ^ 0xFF);
        }

        Cipher aesCipher = Cipher.getInstance(WhatsAppFlowConstant.AES_CIPHER);
        aesCipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(aesKey, WhatsAppFlowConstant.AES_ALGO),
                new GCMParameterSpec(WhatsAppFlowConstant.GCM_TAG_LENGTH_BITS, flippedIv)
        );

        byte[] decrypted = aesCipher.doFinal(Base64.getDecoder().decode(encrypted));
        Map<String, Object> parsed = SerializeUtils.toMap(decrypted);

        assertEquals("3", parsed.get("version"));
        assertEquals("Thanks, received", ((Map<?, ?>) parsed.get("data")).get("message"));
    }

    @Test
    void flowResponseReturnsPlainTextBody() {
        WhatsAppFlowService service = new WhatsAppFlowService(new WhatsAppConfig());

        ResponseEntity<String> response = service.flowResponse("cipher-text", 200);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("cipher-text", response.getBody());
        assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
    }

    private WhatsAppFlowService serviceWithPrivateKey(PrivateKey privateKey) throws Exception {
        WhatsAppFlowService service = new WhatsAppFlowService(new WhatsAppConfig());
        Field field = WhatsAppFlowService.class.getDeclaredField("privateKey");
        field.setAccessible(true);
        field.set(service, privateKey);
        return service;
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private byte[] generateAesKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        return generator.generateKey().getEncoded();
    }
}
