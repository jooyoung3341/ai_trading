package com.kr.at.model;

/**
 * 개별 거래 기록
 */
public class Trade {
    private int index;              // 진입 시점 인덱스
    private String direction;       // LONG or SHORT
    private double entryPrice;      // 진입 가격
    private double exitPrice;       // 청산 가격
    private double confidence;      // 예측 확률
    private String predictedLabel;  // 예측 라벨 (UP_ONLY, DOWN_ONLY)
    private String result;          // WIN, LOSS, TIMEOUT
    private double profitPct;       // 수익률 (%)
    private int holdingBars;        // 보유 봉 수

    public Trade() {}

    public Trade(int index, String direction, double entryPrice, double confidence, String predictedLabel) {
        this.index = index;
        this.direction = direction;
        this.entryPrice = entryPrice;
        this.confidence = confidence;
        this.predictedLabel = predictedLabel;
    }

    // Getters and Setters
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public double getEntryPrice() { return entryPrice; }
    public void setEntryPrice(double entryPrice) { this.entryPrice = entryPrice; }

    public double getExitPrice() { return exitPrice; }
    public void setExitPrice(double exitPrice) { this.exitPrice = exitPrice; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getPredictedLabel() { return predictedLabel; }
    public void setPredictedLabel(String predictedLabel) { this.predictedLabel = predictedLabel; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public double getProfitPct() { return profitPct; }
    public void setProfitPct(double profitPct) { this.profitPct = profitPct; }

    public int getHoldingBars() { return holdingBars; }
    public void setHoldingBars(int holdingBars) { this.holdingBars = holdingBars; }

    @Override
    public String toString() {
        return String.format("Trade[idx=%d, %s, entry=%.2f, exit=%.2f, conf=%.1f%%, %s, profit=%.2f%%]",
                index, direction, entryPrice, exitPrice, confidence * 100, result, profitPct);
    }
}
