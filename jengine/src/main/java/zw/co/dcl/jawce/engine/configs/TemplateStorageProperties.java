package zw.co.dcl.jawce.engine.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "template.storage")
public class TemplateStorageProperties {
    private String templatesPath = "templates";
    private String triggersPath = "triggers";
}
