package com.kr.at.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BinanceConfig {

	@Bean
	public RestClient binanceCon(RestClient.Builder builder) {
		return builder
				.baseUrl("https://fapi.binance.com")
				.build();
	}
}
