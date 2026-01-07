package com.kr.at.model;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.kr.at.common.TradingUtil;
import com.kr.at.model.Candle;
import com.kr.at.model.Candles;

public class Candles {

	@Autowired
	private TradingUtil util;
	
	private List<Double> closes;
	private List<Double> high;
	private List<Double> low;
	private List<Long> openTime;
	public List<Double> getCloses() {
		return closes;
	}
	public void setCloses(List<Double> closes) {
		this.closes = closes;
	}
	public List<Double> getHigh() {
		return high;
	}
	public void setHigh(List<Double> high) {
		this.high = high;
	}
	public List<Double> getLow() {
		return low;
	}
	public void setLow(List<Double> low) {
		this.low = low;
	}
	public List<Long> getOpenTime() {
		return openTime;
	}
	public void setOpenTime(List<Long> openTime) {
		this.openTime = openTime;
	}
	public Candles setCandles(List<Candle> list) {
		this.closes = list.stream()
                .map(Candle::getClose)
                .toList();
		this.high = list.stream()
                .map(Candle::getHigh)
                .toList();
		this.low = list.stream()
                .map(Candle::getLow)
                .toList();
		this.openTime = list.stream()
                .map(Candle::getOpenTime)
                .toList(); 
		return this;	
	}
	
}
