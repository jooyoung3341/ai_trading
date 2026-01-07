package com.kr.at.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.kr.at.model.Candle;
import com.kr.at.service.BinanceService;

import jakarta.annotation.PostConstruct;

@Controller
public class TestController {

	@Autowired
	private BinanceService s;
	
	@PostConstruct
	public void test() {
		System.out.println("시작 ===");
		List<Candle> c = s.getCandles("BTCUSDT", "15m", 10);
		for (Candle c1 : c) {
			System.out.println("C1 : " + c1.toString());
		}
		System.out.println("끝 ===");
	}
}
