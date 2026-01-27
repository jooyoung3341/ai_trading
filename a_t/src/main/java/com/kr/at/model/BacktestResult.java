package com.kr.at.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 백테스트 결과
 */
public class BacktestResult {
    private int totalTrades;        // 총 거래 수
    private int wins;               // 승리 횟수
    private int losses;             // 패배 횟수
    private int timeouts;           // 타임아웃 (목표가 미도달)
    private double winRate;         // 승률 (%)
    private double totalReturnPct;  // 총 수익률 (%)
    private double avgProfitPct;    // 평균 수익률 (%)
    private double maxDrawdownPct;  // 최대 손실폭 (%)
    private double profitFactor;    // 총이익 / 총손실
    private List<Trade> trades;     // 개별 거래 목록

    // 설정값
    private String modelName;
    private double targetProfitPct;
    private double stopLossPct;
    private double minConfidence;
    private double feeRate;
    private int maxHoldingBars;

    public BacktestResult() {
        this.trades = new ArrayList<>();
    }

    // 통계 계산
    public void calculate() {
        this.totalTrades = trades.size();
        this.wins = (int) trades.stream().filter(t -> "WIN".equals(t.getResult())).count();
        this.losses = (int) trades.stream().filter(t -> "LOSS".equals(t.getResult())).count();
        this.timeouts = (int) trades.stream().filter(t -> "TIMEOUT".equals(t.getResult())).count();
        
        this.winRate = totalTrades > 0 ? (double) wins / totalTrades * 100 : 0;
        this.totalReturnPct = trades.stream().mapToDouble(Trade::getProfitPct).sum();
        this.avgProfitPct = totalTrades > 0 ? totalReturnPct / totalTrades : 0;

        // Profit Factor 계산
        double totalProfit = trades.stream()
                .filter(t -> t.getProfitPct() > 0)
                .mapToDouble(Trade::getProfitPct)
                .sum();
        double totalLoss = Math.abs(trades.stream()
                .filter(t -> t.getProfitPct() < 0)
                .mapToDouble(Trade::getProfitPct)
                .sum());
        this.profitFactor = totalLoss > 0 ? totalProfit / totalLoss : totalProfit;

        // Max Drawdown 계산
        double cumulative = 0;
        double peak = 0;
        double maxDD = 0;
        for (Trade t : trades) {
            cumulative += t.getProfitPct();
            if (cumulative > peak) peak = cumulative;
            double dd = peak - cumulative;
            if (dd > maxDD) maxDD = dd;
        }
        this.maxDrawdownPct = maxDD;
    }

    // Getters and Setters
    public int getTotalTrades() { return totalTrades; }
    public void setTotalTrades(int totalTrades) { this.totalTrades = totalTrades; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public int getTimeouts() { return timeouts; }
    public void setTimeouts(int timeouts) { this.timeouts = timeouts; }

    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }

    public double getTotalReturnPct() { return totalReturnPct; }
    public void setTotalReturnPct(double totalReturnPct) { this.totalReturnPct = totalReturnPct; }

    public double getAvgProfitPct() { return avgProfitPct; }
    public void setAvgProfitPct(double avgProfitPct) { this.avgProfitPct = avgProfitPct; }

    public double getMaxDrawdownPct() { return maxDrawdownPct; }
    public void setMaxDrawdownPct(double maxDrawdownPct) { this.maxDrawdownPct = maxDrawdownPct; }

    public double getProfitFactor() { return profitFactor; }
    public void setProfitFactor(double profitFactor) { this.profitFactor = profitFactor; }

    public List<Trade> getTrades() { return trades; }
    public void setTrades(List<Trade> trades) { this.trades = trades; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public double getTargetProfitPct() { return targetProfitPct; }
    public void setTargetProfitPct(double targetProfitPct) { this.targetProfitPct = targetProfitPct; }

    public double getStopLossPct() { return stopLossPct; }
    public void setStopLossPct(double stopLossPct) { this.stopLossPct = stopLossPct; }

    public double getMinConfidence() { return minConfidence; }
    public void setMinConfidence(double minConfidence) { this.minConfidence = minConfidence; }

    public double getFeeRate() { return feeRate; }
    public void setFeeRate(double feeRate) { this.feeRate = feeRate; }

    public int getMaxHoldingBars() { return maxHoldingBars; }
    public void setMaxHoldingBars(int maxHoldingBars) { this.maxHoldingBars = maxHoldingBars; }
}
