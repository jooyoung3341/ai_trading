package com.kr.at.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.CloseableThreadContext.Instance;

public class Candle {

    private long openTime;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
    
    public double getMinPrice() {
    	return Math.min(open, close);
    }
    
    public double getMaxPrice() {
	return Math.max(open, close);
    }

    public Candle(long openTime, double open, double high, double low, double close, double volume) {
        this.openTime = openTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
    
	public long getOpenTime() {
		return openTime;
	}
	public void setOpenTime(long openTime) {
		this.openTime = openTime;
	}
	public double getOpen() {
		return open;
	}
	public void setOpen(double open) {
		this.open = open;
	}
	public double getHigh() {
		return high;
	}
	public void setHigh(double high) {
		this.high = high;
	}
	public double getLow() {
		return low;
	}
	public void setLow(double low) {
		this.low = low;
	}
	public double getClose() {
		return close;
	}
	public void setClose(double close) {
		this.close = close;
	}
	public double getVolume() {
		return volume;
	}
	public void setVolume(double volume) {
		this.volume = volume;
	}

	@Override
	public String toString() {
		ZonedDateTime kst = Instant.ofEpochMilli(openTime).atZone(ZoneId.of("Asia/Seoul"));
		return "Candle [openTime=" + kst.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))  + ", open=" + open + ", high=" + high + ", low=" + low + ", close="
				+ close + ", volume=" + volume + "]";
	}
	
	
}
