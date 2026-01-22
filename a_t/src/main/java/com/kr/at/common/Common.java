package com.kr.at.common;

import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class Common {
	
	// 유닉스 타임스탬프(밀리초 기준)를 한국 시간(KST) LocalDateTime으로 변환
	public LocalDateTime toKst(long time) {
	    return Instant.ofEpochMilli(time)           // 밀리초 단위 시간 생성
	            .atZone(ZoneId.of("Asia/Seoul"))     // 한국 시간대(KST)로 변환
	            .toLocalDateTime();                  // LocalDateTime으로 변환하여 반환
	}
	
    public List<Map<String, Object>> setStartEndDate(int days, int chunkDays) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of( "Asia/Seoul"));

        ZonedDateTime start = now.minusDays(days); // 90일 전
        ZonedDateTime end = start.plusDays(chunkDays);

        
        List<Map<String, Object>> list = new ArrayList<>();
        while (start.isBefore(now)) {
        	Map<String, Object> map = new HashMap<>();
        	ZonedDateTime actualEnd = end.isAfter(now) ? now : end;

            map.put("startDate", start.toInstant().toEpochMilli());
            map.put("endDate", actualEnd.toInstant().toEpochMilli());
            
            
            list.add(map);

            // 다음 구간으로 이동
            start = actualEnd;
            end = start.plusDays(chunkDays);
        }

        return list;
    }

    /** (indicator / close) - 1 : 비율 값(0.01 = 1%) */
    public double pctFrom(double close, double indicator) {
        // NaN/Infinity 방지
        if (!Double.isFinite(close) || !Double.isFinite(indicator)) return 0.0;

        // close가 0이면 나눗셈 불가
        if (close == 0.0) return 0.0;

        return (indicator / close) - 1.0;
    }
    
    /** 퍼센트(%) 단위로 보고 싶으면 (1.0 = 1%) */
    public double pctPercent(double close, double indicator) {
    	System.out.println("[pctPercent] close : " + close + " / indi data : " + indicator );
        return pctFrom(close, indicator) * 100.0;
    }
    
    public int horizonBars(int hour, int interval) {
    	return (hour*60)/interval;
    }
    
    public boolean isBlank(String str) {
    	return str == null || str.trim().isEmpty();
    }
    
    
}
