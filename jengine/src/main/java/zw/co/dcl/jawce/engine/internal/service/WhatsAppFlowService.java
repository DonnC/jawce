package zw.co.dcl.jawce.engine.internal.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.api.exceptions.WhatsAppException;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.WhatsAppFlowConstant;
import zw.co.dcl.jawce.engine.model.core.FlowEndpointPayload;
import zw.co.dcl.jawce.engine.model.core.FlowEndpointResponse;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileReader;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class WhatsAppFlowService {
    private final WhatsAppConfig config;
    private PrivateKey privateKey;

    public WhatsAppFlowService(WhatsAppConfig config) {
        this.config = config;
    }

    private static byte[] flipIv(final byte[] iv) {
        final byte[] result = new byte[iv.length];
        for (int i = 0; i < iv.length; i++) {
            result[i] = (byte) (iv[i] ^ 0xFF);
        }
        return result;
    }

    @PostConstruct
    public void init() throws Exception {
        // Register BouncyCastle provider to support encrypted PEMs
        Security.addProvider(new BouncyCastleProvider());
        Path pemPath = Path.of(config.getPrivateKeyPemPath());
        char[] pwd = config.getPrivateKeyPassword() != null ? config.getPrivateKeyPassword().toCharArray() : null;
        this.privateKey = loadPrivateKeyFromPem(pemPath.toString(), pwd);
    }

    public ResponseEntity<String> flowResponse(String encryptedResponse, int statusCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(encryptedResponse, headers, statusCode);
    }

    /**
     * Try to load private key from PEM. Supports:
     * - Encrypted PEM (OpenSSL-style) using BouncyCastle
     * - Unencrypted PKCS#8 PEM
     */
    private PrivateKey loadPrivateKeyFromPem(String pemFilePath, char[] password) throws Exception {
        try (PEMParser pemParser = new PEMParser(new FileReader(pemFilePath))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if(object instanceof PEMEncryptedKeyPair) {
                if(password == null) {
                    throw new WhatsAppException("PEM is encrypted but no password provided");
                }
                PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password);
                PEMKeyPair keyPair = ((PEMEncryptedKeyPair) object).decryptKeyPair(decProv);
                return converter.getKeyPair(keyPair).getPrivate();
            } else if(object instanceof PEMKeyPair) {
                PEMKeyPair keyPair = (PEMKeyPair) object;
                return converter.getKeyPair(keyPair).getPrivate();
            } else {
                // Fallback: treat file as PKCS#8 unencrypted PEM block
                String pem = java.nio.file.Files.readString(Path.of(pemFilePath));
                final String prefix = "-----BEGIN PRIVATE KEY-----";
                final String suffix = "-----END PRIVATE KEY-----";
                if(!pem.contains(prefix)) {
                    throw new WhatsAppException("Expecting unencrypted private key in PKCS8 format starting with " + prefix);
                }
                String privateKeyPEM = pem.replace(prefix, "").replaceAll("[\\r\\n]", "").replace(suffix, "");
                byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encoded);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                return kf.generatePrivate(spec);
            }
        }
    }

    /**
     * Decrypt incoming flow payload using the official approach:
     * - RSA OAEP SHA-256 to unwrap AES key
     * - AES-GCM to decrypt encrypted_flow_data (ciphertext + tag)
     */
    public FlowEndpointResponse decryptPayload(Map<String, Object> encryptedPayload) throws Exception {
        String encryptedFlowDataB64 = (String) encryptedPayload.get("encrypted_flow_data");
        String encryptedAesKeyB64 = (String) encryptedPayload.get("encrypted_aes_key");
        String initialVectorB64 = (String) encryptedPayload.get("initial_vector");

        byte[] flowData = Base64.getDecoder().decode(encryptedFlowDataB64);
        byte[] iv = Base64.getDecoder().decode(initialVectorB64);
        byte[] encryptedAesKey = Base64.getDecoder().decode(encryptedAesKeyB64);

        // Unwrap AES key with RSA OAEP SHA-256
        Cipher rsaCipher = Cipher.getInstance(WhatsAppFlowConstant.RSA_OAEP);
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] aesKey = rsaCipher.doFinal(encryptedAesKey);

        // AES-GCM decrypt (Java expects ciphertext+tag together)
        Cipher aesCipher = Cipher.getInstance(WhatsAppFlowConstant.AES_CIPHER);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(WhatsAppFlowConstant.GCM_TAG_LENGTH_BITS, iv);
        SecretKeySpec aesKeySpec = new SecretKeySpec(aesKey, WhatsAppFlowConstant.AES_ALGO);
        aesCipher.init(Cipher.DECRYPT_MODE, aesKeySpec, gcmSpec);
        byte[] plain = aesCipher.doFinal(flowData);

        return new FlowEndpointResponse(SerializeUtils.castValue(SerializeUtils.toMap(plain), FlowEndpointPayload.class), aesKey, iv);
    }

    /**
     * Encrypt response payload using flipped IV (bytewise XOR 0xFF) and AES-GCM.
     * Returns base64 encoded ciphertext (ciphertext + tag).
     */
    public String encryptResponse(Map<String, Object> responsePayload, FlowEndpointResponse cfg) throws Exception {
        byte[] flippedIv = flipIv(cfg.iv());

        Cipher aesCipher = Cipher.getInstance(WhatsAppFlowConstant.AES_CIPHER);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(WhatsAppFlowConstant.GCM_TAG_LENGTH_BITS, flippedIv);
        SecretKeySpec aesKeySpec = new SecretKeySpec(cfg.aesKey(), WhatsAppFlowConstant.AES_ALGO);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKeySpec, gcmSpec);

        byte[] plaintext = SerializeUtils.toByteArray(responsePayload);
        byte[] ciphertextWithTag = aesCipher.doFinal(plaintext);
        return Base64.getEncoder().encodeToString(ciphertextWithTag);
    }
}
