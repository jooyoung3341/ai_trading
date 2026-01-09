package com.kr.at.model;

public class FeatureRow {
	 private double close;
	 private double volume;
	 private double low;
	 private double high;
	 private double open;
	 private double ema7;
	 private double ema30;
	 private double ema99;
	 private double ssl;
	 // private double rsi;

	 //private double high; // 라벨 생성용
	 // private double low;  // 라벨 생성용

	 public FeatureRow() {}
	    
	 public FeatureRow(double close, double volume, double low, double high, double open, double ema7, double ema30, double ema99, double ssl) {
		 this.close = close;
		 this.volume = volume;
		 this.low = low;
		 this.high = high;
		 this.open = open;
		 this.ema7 = ema7;
		 this.ema30 = ema30;
		 this.ema99 = ema99;
		 this.ssl = ssl;
		 }

	 
	 public double getLow() {return low;}
	 public void setLow(double low) {this.low = low;	}

	 public double getHigh() {return high;}
	 public void setHigh(double high) {this.high = high;}

	 public double getClose() { return close; }
	 public void setClose(double close) { this.close = close; }
	 
	 public double getVolume() { return volume; }
	 public void setVolume(double volume) { this.volume = volume; }

	 public double getOpen() { return open; }
	 public void setOpen(double open) { this.open = open; }

	 public double getEma7() { return ema7; }
	 public void setEma7(double ema7) { this.ema7 = ema7; }

	 public double getEma30() { return ema30; }
	 public void setEma30(double ema30) { this.ema30 = ema30; }

	 public double getEma99() { return ema99; }
	 public void setEma99(double ema99) { this.ema99 = ema99; }

	 public double getSsl() { return ssl; }
	 public void setSsl(double ssl) { this.ssl = ssl; }

	@Override
	public String toString() {
		return "FeatureRow [close=" + close + ", volume=" + volume + ", low=" + low + ", high=" + high + ", open="
				+ open + ", ema7=" + ema7 + ", ema30=" + ema30 + ", ema99=" + ema99 + ", ssl=" + ssl + "]";
	}

	 
}
