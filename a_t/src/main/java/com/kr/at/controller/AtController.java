package com.kr.at.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kr.at.service.BinanceService;

import jakarta.annotation.PostConstruct;

import com.kr.at.common.Common;
import com.kr.at.common.Indicator;
import com.kr.at.model.Candle;
import com.kr.at.model.EnumType;
import com.kr.at.model.Ticker;
import com.kr.at.service.AtService;

@RestController
public class AtController {

	@Value("${model.path}")
	private String model_path;
	
	@Autowired
	private AtService atService;
	@Autowired
	private BinanceService binanceService;
	@Autowired
	private Common common;
	
	//hold : 변동폭 % ex) 0.003 (0.3%) , hour : 몇시간 안에 움직이는지 시간 정함
	@GetMapping("at/learning")
	public Map<String, Object> atLearning(@RequestParam String symbol, @RequestParam String interval
															,@RequestParam double hold, @RequestParam String hour) throws IOException {
		boolean isFailed = false;
		if(common.isBlank(symbol) || common.isBlank(interval) || common.isBlank(hour)) {isFailed = true;}
		if(hold <= 0.000) {isFailed = true;}
		if(isFailed) {
			return Map.of(EnumType.Result.name(), EnumType.Fail.name());
		}
		
		List<Map<String, Object>> sed = common.setStartEndDate(93, 3);
		List<Candle> candleDatas = new ArrayList<>();
		int startIdx = 0;
		for (Map<String, Object> data : sed) {
			List<Candle> candle = binanceService.getCandlesTime(symbol, interval, (long)data.get("startDate"), (long)data.get("endDate"));
			startIdx = candle.size();
			for (Candle c : candle) {
				candleDatas.add(c);
			}
		}
		String toDay = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		int intervalMin = Integer.parseInt(interval.substring(0, interval.length()-1));
																																							//    symbol, 분봉,    등락폭, 시간 ,   학습날짜
		atService.learning(candleDatas, startIdx, common.horizonBars(Integer.parseInt(hour), intervalMin) , hold, symbol+"_"+interval+"_"+hold+"_"+hour+"_"+toDay);
		return Map.of(EnumType.Result.name(), EnumType.Success.name());
	}
	
	@GetMapping("at/predict")
	public Map<String, Object> atPredict(@RequestParam String modelName) throws Exception{
		String[] names = modelName.split("_");
		String symbol = names[0];
		String interval = names[1];
		List<Candle> datas = binanceService.getCandlesLimit(symbol, interval);
		
		return atService.modelPredict(datas, modelName);
	}
	
	@GetMapping("at/modelList")
	public Map<String, Object> atModelList() throws IOException{
		List<String> modelList = atService.modelList(model_path);
		List<String> models = new ArrayList<>();
		for (String s : modelList) {
			String[] strs = s.split("_");
			if(strs[1].equals("15m")) {
				continue;
			}
			models.add(s);
		}
		
		/*List<String> list = new ArrayList<>();
		for (int i = 0; i < modelList.size(); i++) {
			String[] names = modelList.get(i).split("_");
			String name = names[0].substring(0, names[0].length());
			for (int j = i; j < modelList.size(); j++) {
				String[] subNames = modelList.get(j).split("_");
				String subName = subNames[0].substring(0, subNames[0].length());
				if(name.equals(subName)) {
					list.add(modelList.get(i));
					break;
				}
			}	
		}*/
		return Map.of("modelList", models);
	}
						
	@GetMapping("at/allTicker")
	public Map<String, Object> atAllTicker() {
		List<Ticker> tickers = binanceService.getTickers();
		List<String> tickerList = new ArrayList<>();
		for (Ticker t : tickers) {
			tickerList.add(t.getSymbol());
		}
		return Map.of("tickers", tickerList);
	}
	
	@GetMapping("at/modelDelete")
	public Map<String, Object> atModelDel(@RequestParam String modelName) throws IOException{
		return Map.of("result", atService.modelDel(modelName, model_path));
	}
	
	//@PostConstruct
	public void test() throws Exception{
		System.out.println("학습 시작");
		List<Map<String, Object>> sed = common.setStartEndDate(93, 3);
		List<Candle> candleDatas = new ArrayList<>();
		int startIdx = 0;
		for (Map<String, Object> data : sed) {
			List<Candle> candle = binanceService.getCandlesTime("BTCUSDT", "15m", (long)data.get("startDate"), (long)data.get("endDate"));
			startIdx = candle.size();
			for (Candle c : candle) {
				candleDatas.add(c);
			}
		}
		atService.learning(candleDatas, startIdx, 16, 0.01, "btc15m.model");
		System.out.println("학습 종료");
	}
	
	//@PostConstruct
	public void testLoad() throws Exception {
		List<Candle> datas = binanceService.getCandlesLimit("BTCUSDT", "15m");
		Map<String, Object> map = new HashMap<>();
		map = atService.modelPredict(datas, "btc15m.model");
	}
	
	
	//@PostConstruct
	public void testPer() throws Exception {
		System.out.println("지표값 계산");
		List<Candle> candleDatas = new ArrayList<>();
		List<Candle> c = binanceService.getCandlesLimit("BTCUSDT", "15m");
		List<Double> closes = new ArrayList<>();
		for (Candle candle : c) {
			closes.add(candle.getClose());
		}
		double ema7 = Indicator.ema(closes, 30);
		double per = common.pctPercent(closes.get(closes.size()-1), ema7);
		
		System.out.println("지표 종료 : " + per);
	}
}
