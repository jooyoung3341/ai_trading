package com.kr.at.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.kr.at.model.EnumType;
import com.kr.at.model.QqeResult;
import com.kr.at.model.SslShape;


@Component
public class Indicator {

	// Exponential Moving Average (EMA)
	public static double ema(List<Double> data, int period) {
		double alpha = 2.0 / (period + 1.0);
		double emaValue = data.get(0); // 초기값 설정
		for (int i = 1; i < data.size(); i++) {
			emaValue = alpha * data.get(i) + (1 - alpha) * emaValue;
		}
		return emaValue;
	}

	public static List<Double> emaList(List<Double> data, int period) {
	    List<Double> emaList = new ArrayList<>();
	    if (data == null || data.size() < period) return emaList;

	    double multiplier = 2.0 / (period + 1);
	    double ema = data.get(0); // 초기값 = 첫 데이터 (혹은 sma로 설정 가능)
	    emaList.add(ema);

	    for (int i = 1; i < data.size(); i++) {
	        ema = (data.get(i) * multiplier) + (ema * (1 - multiplier));
	        emaList.add(ema);
	    }

	    return emaList;
	}
	
	// Weighted Moving Average (WMA)
	public static double wma(List<Double> data, int period) {
		int size = data.size();
   	 
        double weightedSum = 0.0;
        if(data.size() <= 1) {
        	for (int i = 0; i < period; i++) {
        		weightedSum += data.get(0)*(i+1);
			}
       	 }else {
       		 for (int i = 0; i < period; i++) {
       			 weightedSum += data.get(size - period + i) * (i + 1);
       		 }
       	 }
        
        double sumOfWeights = (period * (period + 1)) / 2.0;
        
        return weightedSum / sumOfWeights;
   }

	// Hull Moving Average (HMA)
	public static double hma(List<Double> data, int period) {

		List<Double> dataArrHma = new ArrayList<>();
		for (int i = 0; i < period; i++) {
			dataArrHma.add(data.get(data.size() - period + i));
		}

		double wmaHalfPeriod = wma(dataArrHma, period / 2);
		double wmaPeriod = wma(dataArrHma, period);

		List<Double> dataArr = new ArrayList<>();
		dataArr.add(2 * wmaHalfPeriod - wmaPeriod);

		return wma(dataArr, (int) Math.sqrt(period));
	}
	
	public static List<Double> hmaList(List<Double> data, int period) {
	    List<Double> hmaValues = new ArrayList<>();
	    for (int i = 0; i < data.size(); i++) {
	        if (i < period - 1) {
	            hmaValues.add(data.get(i)); // 초기값 보정
	        } else {
	            List<Double> subData = data.subList(0, i + 1);
	            double hma = hma(subData, period); // 너가 만든 hma(double) 버전 재사용
	            hmaValues.add(hma);
	        }
	    }
	    return hmaValues;
	}
	
	 public static List<Double> rsi(List<Double> closes, int period) {
	        List<Double> rsiList = new ArrayList<>();
	        if (closes == null || closes.size() <= period) return rsiList;

	        double gain = 0, loss = 0;

	        // 초기 평균 gain/loss 계산
	        for (int i = 1; i <= period; i++) {
	            double change = closes.get(i) - closes.get(i - 1);
	            if (change > 0) gain += change;
	            else loss -= change;
	        }

	        gain /= period;
	        loss /= period;

	        double rs = loss == 0 ? 100 : gain / loss;
	        rsiList.add(100 - (100 / (1 + rs))); // 첫 RSI

	        // 이후 RSI 계산
	        for (int i = period + 1; i < closes.size(); i++) {
	            double change = closes.get(i) - closes.get(i - 1);
	            double currentGain = change > 0 ? change : 0;
	            double currentLoss = change < 0 ? -change : 0;

	            gain = ((gain * (period - 1)) + currentGain) / period;
	            loss = ((loss * (period - 1)) + currentLoss) / period;

	            rs = loss == 0 ? 100 : gain / loss;
	            double rsi = 100 - (100 / (1 + rs));
	            rsiList.add(rsi);
	        }

	        return rsiList;
	    }
	 
	 public static double sma(List<Double> data, int period) {
		    if (data.size() < period) return 0;
		    return data.subList(data.size() - period, data.size())
		               .stream()
		               .mapToDouble(d -> d)
		               .average()
		               .orElse(0);
		}
	 //Standard Deviation : 표준 편차
	 public static double stdDev(List<Double> data, int period) {
		    if (data.size() < period) return 0;
		    List<Double> sub = data.subList(data.size() - period, data.size());
		    double mean = sub.stream().mapToDouble(d -> d).average().orElse(0);
		    double variance = sub.stream()
		                         .mapToDouble(val -> Math.pow(val - mean, 2))
		                         .average()
		                         .orElse(0);
		    return Math.sqrt(variance);
		}
	 
//========================================================================
	 
	// True Range
	public static double trueRange(double high, double low, double close) {
		return Math.max(Math.max(Math.abs(high - low), Math.abs(high - close)), Math.abs(low - close));
	}

	// SSL Upper Band
	public static double sslUpperk(List<Double> close, List<Double> high, List<Double> low, int period) {
		double keltma = hma(close, period);

		List<Double> rangeArr = new ArrayList<>();
		for (int i = 0; i < period; i++) {
			double range = trueRange(high.get(i), low.get(i), close.get(i));
			rangeArr.add(range);
		}
		double rangema = ema(rangeArr, period);
		return keltma + rangema * 0.2;
	}

	// SSL Lower Band
	public static double sslLowerk(List<Double> close, List<Double> high, List<Double> low, int period) {
		double keltma = hma(close, period);
		List<Double> rangeArr = new ArrayList<>();
		for (int i = 0; i < period; i++) {
			double range = trueRange(high.get(i), low.get(i), close.get(i));
			rangeArr.add(range);
		}
		double rangema = ema(rangeArr, period);       
		return keltma - rangema * 0.2;
	}
	
	public static List<Double> ssl(List<Double> highList, List<Double> lowList,
								List<Double> closeList, int period) {
		    int size = closeList.size();
		    List<Double> emaHighList = hmaList(highList, period);
		    List<Double> emaLowList = hmaList(lowList, period);

		    List<Double> sslList = new ArrayList<>();
		    int hlv = 0;

		    for (int i = 0; i < size; i++) {
		        double close = closeList.get(i);
		        double emaHigh = emaHighList.get(i);
		        double emaLow = emaLowList.get(i);

		        if (close > emaHigh) hlv = 1;
		        else if (close < emaLow) hlv = -1;
		        // else hlv 유지

		        double ssl = (hlv < 0) ? emaHigh : emaLow;
		        sslList.add(ssl);
		    }

		    return sslList;
		}
	
	public static double sslLast(List<Double> highList, List<Double> lowList, List<Double> closeList, int period) {
			int size = closeList.size();
			if (size < period) throw new IllegalArgumentException("Not enough data");
			
			List<Double> emaHighList = hmaList(highList, period);
			List<Double> emaLowList = hmaList(lowList, period);
			
			int hlv = 0;
			double ssl = 0.0;
			
			for (int i = 0; i < size; i++) {
			double close = closeList.get(i);
			double emaHigh = emaHighList.get(i);
			double emaLow = emaLowList.get(i);
			
			if (close > emaHigh) hlv = 1;
			else if (close < emaLow) hlv = -1;
			// else 유지
			
			ssl = (hlv < 0) ? emaHigh : emaLow;
			}
			
			return ssl;
			}
	
	//================================================
																		      // 12, 					10, 							6.0
	 public static QqeResult qqe(List<Double> close, int rsiLength, int smoothingLength, double qqeFactor) {
	        List<Double> rsi = rsi(close, rsiLength);
	        List<Double> smoothedRsi = emaList(rsi, smoothingLength);

	        List<Double> atrRsi = new ArrayList<>();
	        for (int i = 1; i < smoothedRsi.size(); i++) {
	            atrRsi.add(Math.abs(smoothedRsi.get(i) - smoothedRsi.get(i - 1)));
	        }

	        List<Double> smoothedAtr = emaList(atrRsi, rsiLength * 2 - 1);
	        List<Double> dynAtr = smoothedAtr.stream().map(val -> val * qqeFactor).toList();

	        double longBand = 0;
	        double shortBand = 0;
	        int trend = 0;
	        double trendLine = 0;

	        for (int i = 1; i < dynAtr.size(); i++) {
	            double rsiVal = smoothedRsi.get(i + 1);
	            double delta = dynAtr.get(i);

	            double newLong = rsiVal - delta;
	            double newShort = rsiVal + delta;

	            longBand = rsiVal > longBand ? Math.max(longBand, newLong) : newLong;
	            shortBand = rsiVal < shortBand ? Math.min(shortBand, newShort) : newShort;

	            if (rsiVal > shortBand) trend = 1;
	            else if (rsiVal < longBand) trend = -1;

	            trendLine = trend == 1 ? longBand : shortBand;
	        }

	        QqeResult result = new QqeResult();
	        result.trendLine = trendLine;
	        result.smoothedRsi = smoothedRsi.get(smoothedRsi.size() - 1);
	        return result;
	    }
	 
	 public static SslShape detailSsl(List<Double> sslList) {
		 if (sslList == null || sslList.size() < 3) return SslShape.NONE;

		    int third = sslList.size() / 3;

		    double leftAvg = avg(sslList.subList(0, third));
		    double midAvg = avg(sslList.subList(third, 2 * third));
		    double rightAvg = avg(sslList.subList(2 * third, sslList.size()));

		    if (midAvg > leftAvg && midAvg > rightAvg) {
		        return SslShape.PEAK; // ^ 모양
		    } else if (midAvg < leftAvg && midAvg < rightAvg) {
		        return SslShape.VALLEY; // v 모양
		    } else {
		        return SslShape.NONE;
		    }
	 }
	 
	 private static double avg(List<Double> list) {
		    return list.stream().mapToDouble(d -> d).average().orElse(0.0);
		}
	 
	 //=============================
	 //																					88						52						63
	 //																					32						26						50
	 //																					80						27						50	 
	  /*public static Mono<String> getSTC(List<Double> closePrices, int stcLength, int fastLength, int slowLength) {
	        return Mono.fromSupplier(() -> {
	            int size = closePrices.size();
	            if (size < slowLength + 2) { // 최소한 STC 2개 비교할 수 있어야 함
	                throw new IllegalArgumentException("Not enough data for STC color signal detection");
	            }

	            double alpha = 0.5; // smoothing factor

	            List<Double> macdList = calculateMACD(closePrices, fastLength, slowLength);
	            List<Double> k1List = calculateStochastic(macdList, stcLength);
	            List<Double> smoothedK1List = calculateSmoothedEMA(k1List, alpha);

	            List<Double> k2List = calculateStochastic(smoothedK1List, stcLength);
	            List<Double> stcList = calculateSmoothedEMA(k2List, alpha);

	            double currentStc = stcList.get(stcList.size() - 1);
	            double previousStc = stcList.get(stcList.size() - 2);

	            if (currentStc > previousStc) {
	                return EnumType.Long.value();  // 초록색 (green)
	            } else {
	                return EnumType.Short.value(); // 빨간색 (red)
	            }
	        });
	    }*/
	  

	    public static List<Double> calculateEMA(List<Double> data, int period) {
	        List<Double> emaList = new ArrayList<>();
	        double multiplier = 2.0 / (period + 1);
	        double ema = data.get(0); // 초기값은 첫 번째 값
	        
	        for (int i = 0; i < data.size(); i++) {
	            double price = data.get(i);
	            ema = (price - ema) * multiplier + ema;
	            emaList.add(ema);
	        }
	        return emaList;
	    }
	    
	    public static List<Double> calculateMACD(List<Double> closePrices, int fastPeriod, int slowPeriod) {
	        List<Double> fastEMA = calculateEMA(closePrices, fastPeriod);
	        List<Double> slowEMA = calculateEMA(closePrices, slowPeriod);
	        List<Double> macd = new ArrayList<>();
	        
	        for (int i = 0; i < closePrices.size(); i++) {
	            macd.add(fastEMA.get(i) - slowEMA.get(i));
	        }
	        return macd;
	    }

	    public static List<Double> calculateStochastic(List<Double> data, int period) {
	        List<Double> stochasticList = new ArrayList<>();
	        
	        for (int i = 0; i < data.size(); i++) {
	            if (i < period) {
	                stochasticList.add(50.0); // 초기값 (Pine Script에서 초기값 처리 비슷하게)
	            } else {
	                List<Double> subList = data.subList(i - period, i + 1);
	                double lowest = Collections.min(subList);
	                double highest = Collections.max(subList);
	                double value = highest - lowest == 0 ? 50.0 : (data.get(i) - lowest) / (highest - lowest) * 100.0;
	                stochasticList.add(value);
	            }
	        }
	        return stochasticList;
	    }

	    public static List<Double> calculateSmoothedEMA(List<Double> data, double alpha) {
	        List<Double> smoothedList = new ArrayList<>();
	        double prev = data.get(0);
	        
	        for (double value : data) {
	            prev = prev + alpha * (value - prev);
	            smoothedList.add(prev);
	        }
	        return smoothedList;
	    }
}
