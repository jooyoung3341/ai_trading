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
import com.kr.at.model.BacktestResult;
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

	// ==================== 백테스트 API ====================

	/**
	 * 서버 시작 시 자동 백테스트 (주석 해제하면 실행됨)
	 */
	@PostConstruct
	public void backtestOnStartup() {
		try {
			System.out.println("========== [AUTO BACKTEST START] ==========");
			BacktestResult result = atService.runBacktest(
					"BTCUSDT_5m_0.005_4_2026-01-26",  // modelName
					"BTCUSDT",                       // symbol
					"5m",                            // interval
					30,                              // days
					1.0,                             // targetProfitPct
					1.0,                             // stopLossPct
					0.6,                             // minConfidence
					0.0004,                          // feeRate
					48                               // maxHoldingBars
			);
			System.out.println("========== [AUTO BACKTEST END] ==========");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 백테스트 실행 API
	 * 
	 * 예시 호출:
	 * GET /at/backtest/run?modelName=BTCUSDT_5m_0.01_1_2026-01-22
	 *                     &symbol=BTCUSDT
	 *                     &interval=5m
	 *                     &days=30
	 *                     &targetProfitPct=1.0
	 *                     &stopLossPct=1.0
	 *                     &minConfidence=0.6
	 *                     &maxHoldingBars=48
	 */
	@GetMapping("at/backtest/run")
	public Map<String, Object> backtestRun(
			@RequestParam String modelName,
			@RequestParam(defaultValue = "BTCUSDT") String symbol,
			@RequestParam(defaultValue = "5m") String interval,
			@RequestParam(defaultValue = "30") int days,
			@RequestParam(defaultValue = "1.0") double targetProfitPct,
			@RequestParam(defaultValue = "1.0") double stopLossPct,
			@RequestParam(defaultValue = "0.6") double minConfidence,
			@RequestParam(defaultValue = "0.0004") double feeRate,
			@RequestParam(defaultValue = "48") int maxHoldingBars
	) {
		Map<String, Object> response = new HashMap<>();

		try {
			BacktestResult result = atService.runBacktest(
					modelName, symbol, interval, days,
					targetProfitPct, stopLossPct, minConfidence, feeRate, maxHoldingBars
			);

			response.put("status", "SUCCESS");
			response.put("summary", buildBacktestSummary(result));
			response.put("trades", result.getTrades());
			response.put("config", buildBacktestConfig(result));

		} catch (Exception e) {
			response.put("status", "ERROR");
			response.put("message", e.getMessage());
			e.printStackTrace();
		}

		return response;
	}

	/**
	 * 간단 요약 백테스트 (거래 목록 제외)
	 */
	@GetMapping("at/backtest/summary")
	public Map<String, Object> backtestSummary(
			@RequestParam String modelName,
			@RequestParam(defaultValue = "BTCUSDT") String symbol,
			@RequestParam(defaultValue = "5m") String interval,
			@RequestParam(defaultValue = "30") int days,
			@RequestParam(defaultValue = "1.0") double targetProfitPct,
			@RequestParam(defaultValue = "1.0") double stopLossPct,
			@RequestParam(defaultValue = "0.6") double minConfidence,
			@RequestParam(defaultValue = "0.0004") double feeRate,
			@RequestParam(defaultValue = "48") int maxHoldingBars
	) {
		Map<String, Object> response = new HashMap<>();

		try {
			BacktestResult result = atService.runBacktest(
					modelName, symbol, interval, days,
					targetProfitPct, stopLossPct, minConfidence, feeRate, maxHoldingBars
			);

			response.put("status", "SUCCESS");
			response.put("summary", buildBacktestSummary(result));
			response.put("config", buildBacktestConfig(result));

		} catch (Exception e) {
			response.put("status", "ERROR");
			response.put("message", e.getMessage());
			e.printStackTrace();
		}

		return response;
	}

	private Map<String, Object> buildBacktestSummary(BacktestResult result) {
		Map<String, Object> summary = new HashMap<>();
		summary.put("totalTrades", result.getTotalTrades());
		summary.put("wins", result.getWins());
		summary.put("losses", result.getLosses());
		summary.put("timeouts", result.getTimeouts());
		summary.put("winRate", String.format("%.2f%%", result.getWinRate()));
		summary.put("totalReturnPct", String.format("%.2f%%", result.getTotalReturnPct()));
		summary.put("avgProfitPct", String.format("%.2f%%", result.getAvgProfitPct()));
		summary.put("maxDrawdownPct", String.format("%.2f%%", result.getMaxDrawdownPct()));
		summary.put("profitFactor", String.format("%.2f", result.getProfitFactor()));
		return summary;
	}

	private Map<String, Object> buildBacktestConfig(BacktestResult result) {
		Map<String, Object> config = new HashMap<>();
		config.put("modelName", result.getModelName());
		config.put("targetProfitPct", result.getTargetProfitPct() + "%");
		config.put("stopLossPct", result.getStopLossPct() + "%");
		config.put("minConfidence", (result.getMinConfidence() * 100) + "%");
		config.put("feeRate", (result.getFeeRate() * 100) + "%");
		config.put("maxHoldingBars", result.getMaxHoldingBars());
		return config;
	}
}
