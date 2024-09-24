package zw.co.dcl.jawce.engine.model;
import lombok.Data;


@Data
public class WhatsappSettings {
    private boolean isLocalTesting = false;
    private String localTestingRequestUrl;
    private String hubToken;
    private String accessToken;
    private String phoneNumberId;
    private String apiVersion = "v18.0";
}
