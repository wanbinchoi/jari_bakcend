package com.project.jari.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "seoul.api")
@Component
public class SeoulApiProperties {
    private String key;
    private String baseUrl;
    private String serviceName;
    private String responseType;
}
