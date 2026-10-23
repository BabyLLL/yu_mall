package com.macro.mall.portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WechatHttpConfig {
    @Bean
    public RestTemplate wechatRestTemplate() {
        return new RestTemplate();
    }
}
