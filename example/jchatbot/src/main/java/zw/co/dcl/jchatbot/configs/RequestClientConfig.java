package zw.co.dcl.jchatbot.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RequestClientConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
