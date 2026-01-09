package com.kr.at.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.kr.at.model.Candle;

@Service
public class BinanceService {

	private static final Logger log = LoggerFactory.getLogger(BinanceService.class);
	private RestClient restClient;
	
    public BinanceService(RestClient binanceRestClient) {
        this.restClient = binanceRestClient;
    }
    //int limit
	public List<Candle> getCandlesTime(String symbol, String interval, long startTime, long endTime){
		try {
			List<List<Object>> raw = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/fapi/v1/klines")
							.queryParam("symbol", symbol)
							.queryParam("interval", interval)
							.queryParam("limit", 1000)
							.queryParam("startTime", startTime)
							.queryParam("endTime", endTime)
							.build())
					.retrieve()
					.body(new ParameterizedTypeReference<List<List<Object>>>() {});
			if(raw == null) return List.of();
			return mapToCandles(raw);
			
		} catch (Exception e) {
			log.error("[BinanceService - getCandles] Exception {}", e.getMessage());
			return List.of();
		}
	}
	
	public List<Candle> getCandlesLimit(String symbol, String interval){
		try {
			List<List<Object>> raw = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/fapi/v1/klines")
							.queryParam("symbol", symbol)
							.queryParam("interval", interval)
							.build())
					.retrieve()
					.body(new ParameterizedTypeReference<List<List<Object>>>() {});
			if(raw == null) return List.of();
			return mapToCandles(raw);
			
		} catch (Exception e) {
			log.error("[BinanceService - getCandles] Exception {}", e.getMessage());
			return List.of();
		}
	}
	
	
    private List<Candle> mapToCandles(List<List<Object>> raw) {
        return raw.stream().map(data -> {
        	long openTime = ((Number) data.get(0)).longValue();     // 캔들 생성 시간
            double open   = Double.parseDouble(data.get(1).toString());
            double high   = Double.parseDouble(data.get(2).toString());
            double low    = Double.parseDouble(data.get(3).toString());
            double close  = Double.parseDouble(data.get(4).toString());
            double volume = Double.parseDouble(data.get(5).toString());
            return new Candle(openTime, open, high, low, close, volume);
        }).toList();
    }
}
