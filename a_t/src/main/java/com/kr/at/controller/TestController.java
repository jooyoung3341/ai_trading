package com.kr.at.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;

import com.kr.at.common.Common;
import com.kr.at.common.Indicator;
import com.kr.at.model.Candle;
import com.kr.at.model.FeatureRow;
import com.kr.at.service.BinanceService;
import com.kr.at.service.TribuoService;

import jakarta.annotation.PostConstruct;

@Controller
public class TestController {

	@Autowired
	private BinanceService s;
	@Autowired
	private Indicator indicator;
	@Autowired
	private Common com;
	@Autowired
	TribuoService service;
	
	//@PostConstruct
	public void testStart() throws Exception {		
		List<Candle> c = s.getCandlesLimit("BTCUSDT", "15m");
		List<Double> closeData = new ArrayList<>();
		List<Double> highData = new ArrayList<>();
		List<Double> lowData = new ArrayList<>();
		double vol = 0.0;
		int idx = 0;
		for (Candle data : c) {
			closeData.add(data.getClose());
			highData.add(data.getHigh());
			lowData.add(data.getLow());
			vol = data.getVolume();

		}
		
		
		double ema7 = indicator.ema(closeData, 7);
		double ema30 = indicator.ema(closeData, 30);
		double ema99 = indicator.ema(closeData, 99);
		double ssl = indicator.sslLowerk(closeData, highData, lowData, 60);
		
		FeatureRow fr = new FeatureRow();
		fr.setClose(closeData.get(closeData.size()-1));
		fr.setEma7(ema7);
		fr.setEma30(ema30);
		fr.setEma99(ema99);
		fr.setSsl(ssl);
		fr.setVolume(vol);
		
		Path moelPath = Path.of("C:\\Users\\admin\\git\\ai_trading\\a_t\\models\\btc15m.model");
		Model<Label> model = service.loadModel(moelPath);
		
		Prediction<Label> pred = service.predict(model, fr);
		
		service.printResult(pred);
		
	}
	
	//@PostConstruct
	public void test() throws IOException {		
		System.out.println("시작 ===");
		
		List<Double> closeData = new ArrayList<>();
		List<Double> highData = new ArrayList<>();
		List<Double> lowData = new ArrayList<>();
		List<Double> volData = new ArrayList<>();
		List<Map<String, Object>> l = com.setStartEndDate(93, 3);
		System.out.println("l : " + l.size());
		int startIdx = 0;
		for (Map<String, Object> m : l) {
			//System.out.println("l /// 시작날짜 : " + com.toKst((long) m.get("startDate")) + " / " + "종료날짜 : " + com.toKst((long)m.get("endDate")));
			List<Candle> c = s.getCandlesTime("BTCUSDT", "15m", (long)m.get("startDate"), (long)m.get("endDate"));
			startIdx = c.size();
			//System.out.println("startIdx : " + startIdx);
			for (Candle c1 : c) {
				closeData.add(c1.getClose());
				highData.add(c1.getHigh());
				lowData.add(c1.getLow());
				volData.add(c1.getVolume());
			}
		}
		startIdx++;
		List<Double> ema7List = new ArrayList<>();
		List<Double> ema30List = new ArrayList<>();
		List<Double> ema99List = new ArrayList<>();
		
		for (int i = 0; i < closeData.size(); i++) {
			if(i == closeData.size()-startIdx) {
				break;
			}
			List<Double> list = closeData.subList(0, closeData.size()-i);
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
		System.out.println("ema7 size : " + ema7List.size() + " / " + ema7List.get(ema7List.size()-1));
		System.out.println("ema30 size : " + ema30List.size() + " / " + ema30List.get(ema30List.size()-1));
		System.out.println("ema99 size : " + ema99List.size() + " / " + ema99List.get(ema99List.size()-1));
		
		

		
		List<Double> closeDatas = closeData.subList(0, closeData.size()-startIdx);
		List<Double> volDatas = volData.subList(0, volData.size()-startIdx);
		
		List<Double> ssls = new ArrayList<>();
		
		for (int j = 0; j < closeData.size(); j++) {
			if(closeData.size()-j <= 60) {
				break;
			}
			double sslLow = indicator.sslLowerk(highData.subList(0, highData.size()-j), lowData.subList(0, lowData.size()-j), closeData.subList(0, closeData.size()-j), 60);
			ssls.add(sslLow);
		}
		
		List<Double> sslList = ssls.subList(0, ssls.size()-(startIdx-60));
		Collections.reverse(sslList);
		
		List<FeatureRow> fr = new ArrayList<>();
		System.out.println("closeData : " + closeDatas.size());
		System.out.println("ssl : " + sslList.size());
		System.out.println("vol : " + volDatas.get(volDatas.size()-1));

		
		for (int k = 0; k < ema7List.size(); k++) {
			FeatureRow row = new FeatureRow();
			row.setEma30(ema30List.get(k));
			row.setEma7(ema7List.get(k));
			row.setEma99(ema99List.get(k));
			row.setSsl(sslList.get(k));
			row.setClose(closeDatas.get(k));
			row.setVolume(volDatas.get(k));
			fr.add(row);
		}
		
		Path save = service.trainAndSaveTouch4WayModel(fr, 4, 0.01, 0, Path.of("models", "mvp.model"));
		
		System.out.println("성/실 : " + save.toAbsolutePath());
		System.out.println("끝 ===");
		
		
		
	}
}
