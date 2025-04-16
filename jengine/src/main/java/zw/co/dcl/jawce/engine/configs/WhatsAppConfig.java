package zw.co.dcl.jawce.engine.configs;
import lombok.Data;


@Data
public class WhatsAppConfig {
    private boolean local = false;
    private String localUrl;
    private String hubToken;
    private String accessToken;
    private String phoneNumberId;
    private String appSecret;
    private String apiVersion = "v22.0";
}
