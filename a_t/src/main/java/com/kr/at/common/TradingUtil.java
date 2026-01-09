package com.kr.at.common;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

@Component
public class TradingUtil {


	// 기준 가격(basePrice)에 대해 주어진 퍼센트(percent)만큼 변동된 가격을 계산
	public double calculatePercentageChange(double basePrice, double percent) {
	    return basePrice + (basePrice * percent / 100.0);
	}

	// 현재 가격(price)에서 지정된 퍼센트(percenter)만큼 **감소**한 가격을 계산
	public double minusPercent(double price, double percenter) {
	    return calculatePercentageChange(price, -percenter); // 음수로 전달해서 감소
	}

	// 현재 가격(price)에서 지정된 퍼센트(percenter)만큼 **증가**한 가격을 계산
	public double plusPercent(double price, double percenter) {
	    return calculatePercentageChange(price, percenter);  // 양수로 전달해서 증가
	}
														//진입가					//현재가
	public double calculatePercentage(double entryPrice, double currentPrice) {
	    if (entryPrice == 0) {
	        throw new IllegalArgumentException("Entry price cannot be zero.");
	    }
	    return ((currentPrice - entryPrice) / entryPrice) * 100;
	}
}
