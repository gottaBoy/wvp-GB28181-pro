package com.genersoft.iot.vmp.gb28181.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 车辆HTTP客户端配置
 * @author auto-generated
 */
@Configuration
public class VehicleHttpClientConfig {

    /**
     * 配置RestTemplate用于调用车辆端HTTP API
     */
    @Bean
    public RestTemplate vehicleRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 连接超时10秒
        factory.setReadTimeout(30000); // 读取超时30秒
        
        return new RestTemplate(factory);
    }
}