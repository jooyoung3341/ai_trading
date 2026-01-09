package com.kr.at.model;

public class TribuoRow {
	 private double close;
	 private double low;
	 private double high;
	 private double volume;
	 private double ema7;
	 private double ema30;
	 private double ema99;
	 private double ssl;

	 //private double rsi;
	 private String label; // "UP_ONLY" | "DOWN_ONLY" | "NONE" | "BOTH"

	 public TribuoRow() {}

	 public TribuoRow(double close, double low, double high, double volume, double ema7, double ema30, double ema99, double ssl, String label) {
	        this.close = close;
	        this.low = low;
	        this.high = high;
	        this.volume = volume;
	        this.ema7 = ema7;
	        this.ema30 = ema30;
	        this.ema99 = ema99;
	        this.ssl = ssl;
	        this.label = label;
	    }

	    public double getClose() { return close; }
	    public double getLow() { return low; }
	    public double getHigh() { return high; }
	    public double getVolume() { return volume; }
	    public double getEma7() { return ema7; }
	    public double getEma30() { return ema30; }
	    public double getEma99() { return ema99; }
	    public double getSsl() { return ssl; }
	    public String getLabel() { return label; }
}
