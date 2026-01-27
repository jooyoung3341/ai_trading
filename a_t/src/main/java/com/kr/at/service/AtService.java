package com.kr.at.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;

import com.kr.at.common.Indicator;
import com.kr.at.model.BacktestResult;
import com.kr.at.model.Candle;
import com.kr.at.model.FeatureRow;
import com.kr.at.model.Trade;

@Service
public class AtService {

	private static final Logger log = LoggerFactory.getLogger(AtService.class);

	@Autowired
	private TribuoService tribuoService;
	@Autowired
	private Indicator indicator;
	@Autowired
	private BinanceService binanceService;
	
	public Path learning(List<Candle> datas, int startIdx, int bars, double hold, String modelName) throws IOException {
		
		List<Double> closeList = new ArrayList<>();
		List<Double> highList = new ArrayList<>();
		List<Double> lowList = new ArrayList<>();
		List<Double> openList = new ArrayList<>();
		List<Double> volList = new ArrayList<>();
		
		for (Candle data : datas) {
			closeList.add(data.getClose());
			highList.add(data.getHigh());
			lowList.add(data.getLow());
			openList.add(data.getOpen());
			volList.add(data.getVolume());
		}
		
		List<Double> ema7List = new ArrayList<>();
		List<Double> ema30List = new ArrayList<>();
		List<Double> ema99List = new ArrayList<>();
		for (int i = 0; i < closeList.size(); i++) {
			if(i == closeList.size()-startIdx) {
				break;
			}
			List<Double> list = closeList.subList(1, closeList.size()-i);
			double tmpEma = indicator.ema(list, 7);
			ema7List.add(tmpEma);
			tmpEma = indicator.ema(list, 30);
			ema30List.add(tmpEma);
			tmpEma = indicator.ema(list, 99);
			ema99List.add(tmpEma);
		}
		Collections.reverse(ema7List);
		Collections.reverse(ema30List);
		Collections.reverse(ema99List);
		
		List<Double> closeData = closeList.subList(1, closeList.size()-startIdx);
		List<Double> volData = volList.subList(1, volList.size()-startIdx);
		List<Double> highData = highList.subList(1, highList.size()-startIdx);
		List<Double> lowData = lowList.subList(1, lowList.size()-startIdx);
		List<Double> openData = openList.subList(1, openList.size()-startIdx);
		
		List<Double> tmpSslList = new ArrayList<>();
		for (int i = 0; i < closeList.size(); i++) {
			if(closeList.size()-i <= 60) {
				break;
			}
			double sslLow = indicator.sslLowerk(closeList.subList(0, closeList.size()-i), highList.subList(0, highList.size()-i), lowList.subList(0, lowList.size()-i), 60);
			tmpSslList.add(sslLow);
		}
		
		List<Double> sslList = tmpSslList.subList(1, tmpSslList.size()-(startIdx-60));
		Collections.reverse(sslList);
		
		System.out.println("[learning] Start === input Data==========");
		System.out.println("Candle List Size Close : " + closeData.size() + " / Volume : " + volData.size() + " / High : " + highData.size() + " / Low : " + lowData.size());
		System.out.println("Indicator List Size ema7 : " + ema7List.size() + " / ema30 : " + ema30List.size() + " / ema99: " + ema7List.size() + " / Ssl : " + sslList.size());
		System.out.println("Candle first Close : " + closeData.get(0) + " / Volume : " + volData.get(0) + " / High : " + highData.get(0) + " / Low : " + lowData.get(0));
		System.out.println("Indicator List Size ema7 : " + ema7List.get(0) + " / ema30 : " + ema30List.get(0) + " / ema99: " + ema99List.get(0) + " / Ssl : " + sslList.get(0));
		System.out.println("Candle Last Close : " + closeData.get(closeData.size()-1) + " / Volume : " + volData.get(volData.size()-1) + " / High : " + highData.get(highData.size()-1) + " / Low : " + lowData.get(lowData.size()-1));
		System.out.println("Indicator Last Size ema7 : " + ema7List.get(ema7List.size()-1) + " / ema30 : " + ema30List.get(ema30List.size()-1) + " / ema99: " + ema99List.get(ema99List.size()-1) + " / Ssl : " + sslList.get(sslList.size()-1));
		System.out.println("Candle MIN Close : " + closeData.stream().mapToDouble(Double::doubleValue).min().orElseThrow() + " / Volume : " + volData.stream().mapToDouble(Double::doubleValue).min().orElseThrow() + 
									" / High : " + highData.stream().mapToDouble(Double::doubleValue).min().orElseThrow() + " / Low : " + lowData.stream().mapToDouble(Double::doubleValue).min().orElseThrow());
		System.out.println("Indicator MIN Size ema7 : " + ema7List.stream().mapToDouble(Double::doubleValue).min().orElseThrow() + " / ema30 : " + ema30List.stream().mapToDouble(Double::doubleValue).min().orElseThrow() + 
									" / ema99: " + ema99List.stream().mapToDouble(Double::doubleValue).min().orElseThrow() + " / Ssl : " + sslList.stream().mapToDouble(Double::doubleValue).min().orElseThrow());
		System.out.println("Candle MAX Close : " + closeData.stream().mapToDouble(Double::doubleValue).max().orElseThrow() + " / Volume : " + volData.stream().mapToDouble(Double::doubleValue).max().orElseThrow() + 
									" / High : " + highData.stream().mapToDouble(Double::doubleValue).max().orElseThrow() + " / Low : " + lowData.stream().mapToDouble(Double::doubleValue).max().orElseThrow());
		System.out.println("Indicator MAX Size ema7 : " + ema7List.stream().mapToDouble(Double::doubleValue).max().orElseThrow() + " / ema30 : " + ema30List.stream().mapToDouble(Double::doubleValue).max().orElseThrow() + 
									" / ema99: " + ema99List.stream().mapToDouble(Double::doubleValue).max().orElseThrow() + " / Ssl : " + sslList.stream().mapToDouble(Double::doubleValue).max().orElseThrow());
		System.out.println("[learning] End === input Data==========");
		
		List<FeatureRow> frList = new ArrayList<>();
		for (int i = 0; i < closeData.size(); i++) {
			FeatureRow fr = new FeatureRow();
			fr.setEma7(ema7List.get(i));
			fr.setEma30(ema30List.get(i));
			fr.setEma99(ema99List.get(i));
			fr.setSsl(sslList.get(i));
			
			fr.setClose(closeData.get(i));
			fr.setVolume(volData.get(i));
			fr.setLow(lowData.get(i));
			fr.setHigh(highData.get(i));
			fr.setOpen(openData.get(i));
			frList.add(fr);
		}
		//bars = 5분봉 1시간 = 12 / 15분봉 1시간  = 4
		//hold = 0.003(0.3%)
		return tribuoService.trainAndSaveTouch4WayModel(frList, bars, hold, modelName);
	}
	
	//예측
	public Map<String, Object> modelPredict(List<Candle> datas, String modelName) throws Exception{
		List<Double> closeList = new ArrayList<>();
		List<Double> highList = new ArrayList<>();
		List<Double> lowList = new ArrayList<>();
		List<Double> openList = new ArrayList<>();
		
		double volume = 0.0;
		int idx = 0;
		
		for (Candle data : datas) {
			if(idx == datas.size()-1) {
				break;
			}
			closeList.add(data.getClose());
			highList.add(data.getHigh());
			lowList.add(data.getLow());
			openList.add(data.getOpen());
			volume = data.getVolume();
			
			idx++;
		}
		
		double ema7 = indicator.ema(closeList, 7);
		double ema30 = indicator.ema(closeList, 30);
		double ema99 = indicator.ema(closeList, 99);
		double ssl = indicator.sslLowerk(closeList, highList, lowList, 60);
		
		FeatureRow fr = new FeatureRow();
		fr.setClose(closeList.get(closeList.size()-1));
		fr.setLow(lowList.get(lowList.size()-1));
		fr.setHigh(highList.get(highList.size()-1));
		fr.setOpen(openList.get(openList.size()-1));
		fr.setVolume(volume);
		
		fr.setSsl(ssl);
		fr.setEma7(ema7);
		fr.setEma30(ema30);
		fr.setEma99(ema99);
		
		//Path modelPath = Path.of("C:\\Users\\admin\\git\\ai_trading\\a_t\\models\\"+modelName);
		Model<Label> model = tribuoService.loadModel(modelName);
		Prediction<Label> pred = tribuoService.predict(model, fr);
		
		return tribuoService.printResult(pred);
	}
	
	public List<String> modelList(String path) throws IOException{
		Path dir = Paths.get(path);
		
	    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
	        return List.of(); // 또는 예외 throw
	    }

	    try (Stream<Path> stream = Files.list(dir)) {
	        return stream
	                .filter(Files::isRegularFile)
	                .map(Path::getFileName)
	                .map(Path::toString)
	                .filter(name -> name.endsWith(".model"))
	                .sorted()
	                .collect(Collectors.toList());
	    }
	}
	
	public boolean modelDel(String modelName, String path) throws IOException {
        String fileName = modelName.endsWith(".model") ? modelName : modelName + ".model";
		Path base = Paths.get(path).toAbsolutePath().normalize();
		Path target = base.resolve(fileName).normalize();
		return Files.deleteIfExists(target);
	}

	// ==================== 백테스트 관련 메서드 ====================

	/**
	 * 백테스트 실행
	 * @param modelName 모델 이름
	 * @param symbol 심볼 (예: BTCUSDT)
	 * @param interval 봉 간격 (예: 5m, 15m)
	 * @param days 테스트 기간 (일)
	 * @param targetProfitPct 목표 수익률 (예: 1.0 = 1%)
	 * @param stopLossPct 손절 비율 (예: 1.0 = 1%)
	 * @param minConfidence 최소 신뢰도 (예: 0.6 = 60%)
	 * @param feeRate 수수료율 (예: 0.0004 = 0.04%)
	 * @param maxHoldingBars 최대 보유 봉 수
	 */
	public BacktestResult runBacktest(
			String modelName,
			String symbol,
			String interval,
			int days,
			double targetProfitPct,
			double stopLossPct,
			double minConfidence,
			double feeRate,
			int maxHoldingBars
	) throws Exception {

		log.info("========== [BACKTEST START] ==========");
		log.info("[CONFIG] Model: {}", modelName);
		log.info("[CONFIG] Symbol: {}, Interval: {}, Days: {}", symbol, interval, days);
		log.info("[CONFIG] Target: {}%, StopLoss: {}%, MinConfidence: {}%", 
				targetProfitPct, stopLossPct, minConfidence * 100);
		log.info("[CONFIG] FeeRate: {}%, MaxHoldingBars: {}", feeRate * 100, maxHoldingBars);

		// 1. 모델 로드
		log.info("[STEP 1] Loading model...");
		Model<Label> model = tribuoService.loadModel(modelName);
		log.info("[STEP 1] Model loaded successfully");

		// 2. 데이터 수집 (테스트용 데이터)
		log.info("[STEP 2] Collecting candle data...");
		List<Candle> candles = collectCandles(symbol, interval, days);
		log.info("[STEP 2] Collected {} candles", candles.size());
		if (!candles.isEmpty()) {
			log.info("[STEP 2] Price range: {} ~ {}", 
					candles.stream().mapToDouble(Candle::getLow).min().orElse(0),
					candles.stream().mapToDouble(Candle::getHigh).max().orElse(0));
		}

		// 3. FeatureRow 리스트 생성 (지표 계산 포함)
		log.info("[STEP 3] Building feature rows with indicators...");
		List<FeatureRow> featureRows = buildFeatureRowsForBacktest(candles);
		log.info("[STEP 3] Built {} feature rows", featureRows.size());

		// 4. 백테스트 실행
		log.info("[STEP 4] Executing backtest...");
		BacktestResult result = executeBacktest(
				model, featureRows, candles,
				targetProfitPct, stopLossPct, minConfidence, feeRate, maxHoldingBars
		);

		// 5. 설정값 저장
		result.setModelName(modelName);
		result.setTargetProfitPct(targetProfitPct);
		result.setStopLossPct(stopLossPct);
		result.setMinConfidence(minConfidence);
		result.setFeeRate(feeRate);
		result.setMaxHoldingBars(maxHoldingBars);

		// 6. 통계 계산
		result.calculate();

		// 결과 로그
		log.info("========== [BACKTEST RESULT] ==========");
		log.info("[RESULT] Total Trades: {}", result.getTotalTrades());
		log.info("[RESULT] Wins: {}, Losses: {}, Timeouts: {}", 
				result.getWins(), result.getLosses(), result.getTimeouts());
		log.info("[RESULT] Win Rate: {}%", String.format("%.2f", result.getWinRate()));
		log.info("[RESULT] Total Return: {}%", String.format("%.2f", result.getTotalReturnPct()));
		log.info("[RESULT] Avg Profit per Trade: {}%", String.format("%.2f", result.getAvgProfitPct()));
		log.info("[RESULT] Max Drawdown: {}%", String.format("%.2f", result.getMaxDrawdownPct()));
		log.info("[RESULT] Profit Factor: {}", String.format("%.2f", result.getProfitFactor()));
		log.info("========== [BACKTEST END] ==========");

		return result;
	}

	/**
	 * 캔들 데이터 수집 (백테스트용)
	 */
	private List<Candle> collectCandles(String symbol, String interval, int days) {
		List<Candle> allCandles = new ArrayList<>();
		
		// 3일 단위로 데이터 수집 (API 제한 고려)
		long now = System.currentTimeMillis();
		long dayMs = 24 * 60 * 60 * 1000L;
		long startTime = now - (days * dayMs);
		long chunkMs = 3 * dayMs;

		long currentStart = startTime;
		while (currentStart < now) {
			long currentEnd = Math.min(currentStart + chunkMs, now);
			List<Candle> chunk = binanceService.getCandlesTime(symbol, interval, currentStart, currentEnd);
			allCandles.addAll(chunk);
			currentStart = currentEnd;
		}

		return allCandles;
	}

	/**
	 * FeatureRow 리스트 생성 (백테스트용 지표 계산)
	 */
	private List<FeatureRow> buildFeatureRowsForBacktest(List<Candle> candles) {
		List<Double> closeList = new ArrayList<>();
		List<Double> highList = new ArrayList<>();
		List<Double> lowList = new ArrayList<>();

		for (Candle c : candles) {
			closeList.add(c.getClose());
			highList.add(c.getHigh());
			lowList.add(c.getLow());
		}

		List<FeatureRow> featureRows = new ArrayList<>();

		// 최소 100개 이상의 데이터가 필요 (EMA99 + SSL60 안정화)
		int startIdx = 100;

		for (int i = startIdx; i < candles.size(); i++) {
			Candle c = candles.get(i);

			// 현재 시점까지의 데이터로 지표 계산
			List<Double> closeSub = closeList.subList(0, i + 1);
			List<Double> highSub = highList.subList(0, i + 1);
			List<Double> lowSub = lowList.subList(0, i + 1);

			double ema7 = indicator.ema(closeSub, 7);
			double ema30 = indicator.ema(closeSub, 30);
			double ema99 = indicator.ema(closeSub, 99);
			double ssl = indicator.sslLowerk(closeSub, highSub, lowSub, 60);

			FeatureRow fr = new FeatureRow(
					c.getClose(), c.getVolume(), c.getLow(), c.getHigh(), c.getOpen(),
					ema7, ema30, ema99, ssl
			);
			featureRows.add(fr);
		}

		return featureRows;
	}

	/**
	 * 백테스트 실행 로직
	 */
	private BacktestResult executeBacktest(
			Model<Label> model,
			List<FeatureRow> featureRows,
			List<Candle> candles,
			double targetProfitPct,
			double stopLossPct,
			double minConfidence,
			double feeRate,
			int maxHoldingBars
	) {
		BacktestResult result = new BacktestResult();
		List<Trade> trades = new ArrayList<>();

		// 캔들 인덱스 오프셋 (featureRows는 100번째부터 시작)
		int offset = 100;

		int i = 0;
		while (i < featureRows.size() - maxHoldingBars) {
			FeatureRow fr = featureRows.get(i);

			// 예측
			Prediction<Label> pred = tribuoService.predict(model, fr);
			Map<String, Label> scores = pred.getOutputScores();

			// UP_ONLY, DOWN_ONLY 확률 확인
			double upOnlyProb = scores.containsKey("UP_ONLY") ? scores.get("UP_ONLY").getScore() : 0;
			double downOnlyProb = scores.containsKey("DOWN_ONLY") ? scores.get("DOWN_ONLY").getScore() : 0;

			String direction = null;
			double confidence = 0;
			String predictedLabel = null;

			// UP_ONLY가 minConfidence 이상이면 LONG
			if (upOnlyProb >= minConfidence) {
				direction = "LONG";
				confidence = upOnlyProb;
				predictedLabel = "UP_ONLY";
			}
			// DOWN_ONLY가 minConfidence 이상이면 SHORT
			else if (downOnlyProb >= minConfidence) {
				direction = "SHORT";
				confidence = downOnlyProb;
				predictedLabel = "DOWN_ONLY";
			}

			// 진입 조건 충족 시
			if (direction != null) {
				int candleIdx = i + offset;
				double entryPrice = candles.get(candleIdx).getClose();

				Trade trade = new Trade(candleIdx, direction, entryPrice, confidence, predictedLabel);
				
				log.info("[TRADE #{}] {} Entry at {} (confidence: {}%)", 
						trades.size() + 1, direction, String.format("%.2f", entryPrice), 
						String.format("%.1f", confidence * 100));

				// 목표가/손절가 계산
				double targetPrice, stopPrice;
				if ("LONG".equals(direction)) {
					targetPrice = entryPrice * (1 + targetProfitPct / 100);
					stopPrice = entryPrice * (1 - stopLossPct / 100);
				} else {
					targetPrice = entryPrice * (1 - targetProfitPct / 100);
					stopPrice = entryPrice * (1 + stopLossPct / 100);
				}

				// 미래 봉들 확인
				boolean closed = false;
				for (int j = 1; j <= maxHoldingBars && (candleIdx + j) < candles.size(); j++) {
					Candle futureCandle = candles.get(candleIdx + j);
					double high = futureCandle.getHigh();
					double low = futureCandle.getLow();

					if ("LONG".equals(direction)) {
						// LONG: 고가가 목표가 도달 → WIN
						if (high >= targetPrice) {
							trade.setExitPrice(targetPrice);
							trade.setResult("WIN");
							trade.setProfitPct(targetProfitPct - (feeRate * 2 * 100));
							trade.setHoldingBars(j);
							closed = true;
							break;
						}
						// LONG: 저가가 손절가 도달 → LOSS
						if (low <= stopPrice) {
							trade.setExitPrice(stopPrice);
							trade.setResult("LOSS");
							trade.setProfitPct(-stopLossPct - (feeRate * 2 * 100));
							trade.setHoldingBars(j);
							closed = true;
							break;
						}
					} else {
						// SHORT: 저가가 목표가 도달 → WIN
						if (low <= targetPrice) {
							trade.setExitPrice(targetPrice);
							trade.setResult("WIN");
							trade.setProfitPct(targetProfitPct - (feeRate * 2 * 100));
							trade.setHoldingBars(j);
							closed = true;
							break;
						}
						// SHORT: 고가가 손절가 도달 → LOSS
						if (high >= stopPrice) {
							trade.setExitPrice(stopPrice);
							trade.setResult("LOSS");
							trade.setProfitPct(-stopLossPct - (feeRate * 2 * 100));
							trade.setHoldingBars(j);
							closed = true;
							break;
						}
					}
				}

				// 타임아웃 (목표가/손절가 미도달)
				if (!closed) {
					int lastIdx = Math.min(candleIdx + maxHoldingBars, candles.size() - 1);
					double exitPrice = candles.get(lastIdx).getClose();
					trade.setExitPrice(exitPrice);
					trade.setResult("TIMEOUT");
					
					double pnl;
					if ("LONG".equals(direction)) {
						pnl = (exitPrice - entryPrice) / entryPrice * 100;
					} else {
						pnl = (entryPrice - exitPrice) / entryPrice * 100;
					}
					trade.setProfitPct(pnl - (feeRate * 2 * 100));
					trade.setHoldingBars(maxHoldingBars);
				}

				trades.add(trade);
				
				log.info("[TRADE #{}] {} | Entry: {} | Exit: {} | Result: {} | Profit: {}% | Bars: {}",
						trades.size(), direction, 
						String.format("%.2f", entryPrice), 
						String.format("%.2f", trade.getExitPrice()), 
						trade.getResult(), 
						String.format("%.2f", trade.getProfitPct()), 
						trade.getHoldingBars());

				// 다음 진입은 청산 후부터
				i += trade.getHoldingBars();
			}

			i++;
		}

		log.info("[BACKTEST] Total trades executed: {}", trades.size());
		result.setTrades(trades);
		return result;
	}
}
