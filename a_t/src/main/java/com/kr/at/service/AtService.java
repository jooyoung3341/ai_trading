package com.kr.at.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;

import com.kr.at.common.Indicator;
import com.kr.at.model.Candle;
import com.kr.at.model.FeatureRow;

@Service
public class AtService {

	@Autowired
	private TribuoService tribuoService;
	@Autowired
	private Indicator indicator;
	
	public Path learning(List<Candle> datas, int startIdx, int bars, double hold, String modelName) throws IOException {
		
		List<Double> closeList = new ArrayList<>();
		List<Double> highList = new ArrayList<>();
		List<Double> lowList = new ArrayList<>();
		List<Double> volList = new ArrayList<>();
		
		for (Candle data : datas) {
			closeList.add(data.getClose());
			highList.add(data.getHigh());
			lowList.add(data.getLow());
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
			frList.add(fr);
		}
		//bars = 5분봉 1시간 = 12 / 15분봉 1시간  = 4
		//hold = 0.003(0.3%)
		return tribuoService.trainAndSaveTouch4WayModel(frList, bars, hold, 0, Path.of("models",modelName));
	}
	
	public Map<String, Object> modelLoad(List<Candle> datas, String modelName) throws Exception{
		List<Double> closeList = new ArrayList<>();
		List<Double> highList = new ArrayList<>();
		List<Double> lowList = new ArrayList<>();
		double volume = 0.0;
		int idx = 0;
		
		for (Candle data : datas) {
			if(idx == datas.size()-1) {
				break;
			}
			closeList.add(data.getClose());
			highList.add(data.getHigh());
			lowList.add(data.getLow());
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
		fr.setVolume(volume);
		
		fr.setSsl(ssl);
		fr.setEma7(ema7);
		fr.setEma30(ema30);
		fr.setEma99(ema99);
		
		Path modelPath = Path.of("C:\\Users\\admin\\git\\ai_trading\\a_t\\models\\"+modelName);
		Model<Label> model = tribuoService.loadModel(modelPath);
		Prediction<Label> pred = tribuoService.predict(model, fr);
		
		tribuoService.printResult(pred);
		
		return new HashMap<>();
	}
}
