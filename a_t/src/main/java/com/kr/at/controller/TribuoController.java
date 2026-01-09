package com.kr.at.controller;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;

import com.kr.at.service.BinanceService;

import jakarta.annotation.PostConstruct;

import com.kr.at.common.Common;
import com.kr.at.model.Candle;
import com.kr.at.model.FeatureRow;
import com.kr.at.service.AtService;

@Controller
public class TribuoController {

	@Autowired
	private AtService atService;
	@Autowired
	private BinanceService binaceService;
	@Autowired
	private Common common;
	
	@PostConstruct
	public void test() throws Exception{
		System.out.println("학습 시작");
		List<Map<String, Object>> sed = common.setStartEndDate(93, 3);
		List<Candle> candleDatas = new ArrayList<>();
		int startIdx = 0;
		for (Map<String, Object> data : sed) {
			List<Candle> candle = binaceService.getCandlesTime("BTCUSDT", "15m", (long)data.get("startDate"), (long)data.get("endDate"));
			startIdx = candle.size();
			for (Candle c : candle) {
				candleDatas.add(c);
			}
		}
		atService.learning(candleDatas, startIdx, 4, 0.005, "btc15m.model");
		System.out.println("학습 종료");
	}
	
	//@PostConstruct
	public void testLoad() throws Exception {
		List<Candle> datas = binaceService.getCandlesLimit("BTCUSDT", "15m");
		Map<String, Object> map = new HashMap<>();
		map = atService.modelLoad(datas, "btc15m.model");
		
	}
}
