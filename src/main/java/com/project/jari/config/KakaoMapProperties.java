package com.project.jari.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kakao.map")
@Data
public class KakaoMapProperties {
    private String apiKey;
    private String baseUrl;
}