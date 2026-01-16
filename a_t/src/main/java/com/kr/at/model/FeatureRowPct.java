package com.kr.at.model;

public class FeatureRowPct {
	private double highPct;
	private double lowPct;
	private double volLog;     // 거래량은 보통 log1p 추천
	private double ema7Pct;
	private double ema30Pct;
	private double ema99Pct;
	private double sslPct;
	private String label;

    public FeatureRowPct(double highPct, double lowPct, double volLog, double ema7Pct, double ema30Pct, double ema99Pct, double sslPct, String label) {
		this.highPct = highPct;
		this.lowPct = lowPct;
		this.volLog = volLog;
		this.ema7Pct = ema7Pct;
		this.ema30Pct = ema30Pct;
		this.ema99Pct = ema99Pct;
		this.sslPct = sslPct;
		this.label = label;
    }
    
	public double getHighPct() { return highPct; }
	public double getLowPct() { return lowPct; }
	public double getVolLog() { return volLog; }
	public double getEma7Pct() { return ema7Pct; }
	public double getEma30Pct() { return ema30Pct; }
	public double getEma99Pct() { return ema99Pct; }
	public double getSslPct() { return sslPct; }
	public String getLabel() { return label; }
}	
