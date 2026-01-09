package com.kr.at.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;

import com.kr.at.common.Common;
import com.kr.at.common.Indicator;
import com.kr.at.model.FeatureRow;
import com.kr.at.model.TribuoRow;

import org.tribuo.*;
import org.tribuo.impl.ListExample;
import org.tribuo.provenance.SimpleDataSourceProvenance;
import org.tribuo.transform.TransformationMap;
import org.tribuo.transform.transformations.MeanStdDevTransformation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@Service
public class TribuoService {

	@Autowired
	private BinanceService bService;
	@Autowired
	private Indicator indicator;
	@Autowired
	private Common common;
	
	public enum Y { UP_ONLY, DOWN_ONLY, NONE, BOTH}
		
	
    // 피처 이름(이름표??)
    public static final String[] FEATURE_NAMES = {
            "close", "low", "high", "volume", "ema7", "ema30", "ema99", "ssl"
    };
    

    public static Model<Label> loadModel(Path modelPath) throws Exception {
        // 저장했던 model.serializeToFile(modelPath) 의 역방향
        return (Model<Label>) Model.deserializeFromFile(modelPath);
    }

    public static Prediction<Label> predict(Model<Label> model, FeatureRow fr) {
        System.out.println("=== [PREDICT INPUT] =========================");
        System.out.println("input FeatureRow = " + fr);

        double close  = fr.getClose();
        double vol    = fr.getVolume();
        double ema7   = fr.getEma7();
        double ema30  = fr.getEma30();
        double ema99  = fr.getEma99();
        double ssl    = fr.getSsl();

        System.out.printf("close=%.8f vol=%.8f ema7=%.8f ema30=%.8f ema99=%.8f ssl=%.8f%n",
                close, vol, ema7, ema30, ema99, ssl);

        // 이상치/NaN 체크
        if (!Double.isFinite(close) || !Double.isFinite(vol) || !Double.isFinite(ema7) ||
            !Double.isFinite(ema30) || !Double.isFinite(ema99) || !Double.isFinite(ssl)) {
            System.out.println("[WARN] non-finite feature exists!");
        }
        if (close <= 0) System.out.println("[WARN] close <= 0");
        if (vol < 0)    System.out.println("[WARN] volume < 0");

        // 모델이 아는 feature 목록 찍기 (입력 feature 이름 mismatch 잡기 좋음)
        var fmap = model.getFeatureIDMap();
        System.out.println("model features = " + fmap.keySet());

        System.out.println("============================================");
        
    	System.out.println("입력 데이터 : " + fr.toString());
        List<Feature> feats = new ArrayList<>(8);
        feats.add(new Feature(FEATURE_NAMES[0], fr.getClose()));
        feats.add(new Feature(FEATURE_NAMES[1], fr.getLow()));
        feats.add(new Feature(FEATURE_NAMES[2], fr.getHigh()));
        feats.add(new Feature(FEATURE_NAMES[3], fr.getVolume()));
        feats.add(new Feature(FEATURE_NAMES[4], fr.getEma7()));
        feats.add(new Feature(FEATURE_NAMES[5], fr.getEma30()));
        feats.add(new Feature(FEATURE_NAMES[6], fr.getEma99()));
        feats.add(new Feature(FEATURE_NAMES[7], fr.getSsl()));

        // 예측할 때 Output(Label)은 “더미”여도 됨(학습용 정답이 아니라 입력 형태 맞추는 용도)
        Example<Label> ex = new ListExample<>(new Label(Label.UNKNOWN), feats);

        // 피처가 하나도 없거나, 모델이 아는 피처와 겹치는 게 없으면 예외가 날 수 있음 :contentReference[oaicite:1]{index=1}
        return model.predict(ex);
    }

    public static void printResult(Prediction<Label> pred) {
        String predicted = pred.getOutput().getLabel(); // Label의 이름 꺼내기 :contentReference[oaicite:2]{index=2}
        System.out.println("predicted=" + predicted);

        // 각 라벨 점수(또는 확률) 맵 :contentReference[oaicite:3]{index=3}
        Map<String, Label> scores = pred.getOutputScores();
        boolean probs = pred.hasProbabilities(); // 확률인지 여부 :contentReference[oaicite:4]{index=4}

        System.out.println("hasProbabilities=" + probs);
        
        
        double pUp   = pred.getOutputScores().get("UP_ONLY").getScore();   // 0~1
        double pDown = pred.getOutputScores().get("DOWN_ONLY").getScore(); // 0~1
        double pNone = pred.getOutputScores().get("NONE").getScore();
        double pBoth = pred.getOutputScores().get("BOTH").getScore();
        System.out.printf("UP=%.17f DOWN=%.17f NONE=%.17f BOTH=%.17f sum=%.17f%n",
        	    pUp, pDown, pNone, pBoth, (pUp+pDown+pNone+pBoth));
        
        System.out.println("=== [PREDICT OUTPUT] ========================");
        System.out.println("predicted=" + pred.getOutput().getLabel());
        System.out.println("hasProbabilities=" + pred.hasProbabilities());

        // 전체 출력 스코어를 그대로 다 찍어라 (정렬 전 원본도)
        System.out.println("[raw outputScores]");
        pred.getOutputScores().forEach((k,v) ->
                System.out.printf("  %s => %.17f%n", k, v.getScore())
        );
        
        // “너무 확신(포화)” 감지용
        double max = Math.max(Math.max(pUp,pDown), Math.max(pNone,pBoth));
        double min = Math.min(Math.min(pUp,pDown), Math.min(pNone,pBoth));
        System.out.printf("[sharpness] max=%.17f min=%.17f gap=%.17f%n", max, min, (max-min));
        scores.entrySet().stream()
                .sorted((a,b) -> Double.compare(b.getValue().getScore(), a.getValue().getScore()))
                .forEach(e -> System.out.println(e.getKey() + " : " + e.getValue().getScore()));
    }
    
    
    
    /**
     *  전체 흐름:
     * featureRows(이미 지표 계산된 원본) → 라벨 생성 → TrainingRow 리스트 → Dataset → train → save
     */
    public Path trainAndSaveTouch4WayModel(
            List<FeatureRow> featureRows,
            int horizonBars,        // 예: 5분봉 1시간=12
            double threshold,       // 예: 0.003 (0.3%)
            int warmupBars,         // 예: EMA99 안정화로 200
            Path modelPath          // 저장 경로
    ) throws IOException {
    	System.out.println("featureRows : " + featureRows.size());
        if (featureRows == null || featureRows.isEmpty()) {
            throw new IllegalArgumentException("featureRows is empty");
        }
        if (featureRows.size() <= warmupBars + horizonBars) {
            throw new IllegalArgumentException("Not enough rows. size=" + featureRows.size()
                    + ", warmupBars=" + warmupBars + ", horizonBars=" + horizonBars);
        }

        for (int k = 0; k < Math.min(5, featureRows.size()); k++) {
            FeatureRow r = featureRows.get(k);
            System.out.println("[ROW " + k + "] close=" + r.getClose());
        }
        
        int bad = 0;
        for (FeatureRow r : featureRows) {
            if (!Double.isFinite(r.getClose()) ||
                !Double.isFinite(r.getVolume()) ||
                !Double.isFinite(r.getEma7()) ||
                !Double.isFinite(r.getEma30()) ||
                !Double.isFinite(r.getEma99()) ||
                !Double.isFinite(r.getSsl())) {
                bad++;
            }
            if (r.getClose() <= 0) bad++;
        }
        System.out.println("[SANITY] badRows=" + bad + " / " + featureRows.size());
        
        // 1) 라벨 생성해서 TrainingRow 만들기 (정답지 만드는 단계)
       // Touch4WayLabeler labeler = new Touch4WayLabeler();
        List<TribuoRow> trainRows = new ArrayList<TribuoRow>(featureRows.size());

        for (int t = 0; t < featureRows.size(); t++) {
            if (t < warmupBars) continue;
            if (t + horizonBars >= featureRows.size()) break; // 미래를 봐야 라벨 가능

            String y = label(featureRows, t, horizonBars, threshold);

            FeatureRow fr = featureRows.get(t);
            trainRows.add(new TribuoRow(
                    fr.getClose(),
                    fr.getLow(),
                    fr.getHigh(),
                    fr.getVolume(),
                    fr.getEma7(),
                    fr.getEma30(),
                    fr.getEma99(),
                    fr.getSsl(),
                    y
            ));
        }

     //   분포 로그 (Dataset 만들기 전)
        int up=0, down=0, none=0, both=0, other=0;
        for (TribuoRow r : trainRows) {
            String y = r.getLabel();
            if ("UP_ONLY".equals(y)) up++;
            else if ("DOWN_ONLY".equals(y)) down++;
            else if ("NONE".equals(y)) none++;
            else if ("BOTH".equals(y)) both++;
            else other++;
        }
        System.out.println("[LABEL_DIST] total=" + trainRows.size()
                + " UP_ONLY=" + up + " DOWN_ONLY=" + down + " NONE=" + none + " BOTH=" + both + " other=" + other);
        // 2) Dataset 생성
        MutableDataset<Label> dataset = buildDataset(trainRows, "touch4way_H" + horizonBars + "_T" + threshold);

     // Dataset 만든 직후
        TransformationMap tmap = new TransformationMap(
            java.util.List.of(new MeanStdDevTransformation()) // 평균0/표준편차1 :contentReference[oaicite:4]{index=4}
        );

       // var transformerMap = dataset.createTransformers(tmap); // :contentReference[oaicite:5]{index=5}
        //dataset.transform(transformerMap);                     // :contentReference[oaicite:6]{index=6}
        
        // 3) 학습 (MVP: Logistic Regression)
        LogisticRegressionTrainer trainer = new LogisticRegressionTrainer();
        Model<Label> model = trainer.train(dataset);
        
        // 4) 저장
        Files.createDirectories(modelPath.getParent());
        model.serializeToFile(modelPath);

        return modelPath;
    }

    private MutableDataset<Label> buildDataset(List<TribuoRow> rows, String datasourceId) {
        LabelFactory labelFactory = new LabelFactory();

        SimpleDataSourceProvenance prov =
                new SimpleDataSourceProvenance(datasourceId, OffsetDateTime.now(), labelFactory);

        MutableDataset<Label> dataset = new MutableDataset<Label>(prov, labelFactory);

        for (TribuoRow r : rows) {
            Label y = new Label(r.getLabel());

            List<Feature> feats = new ArrayList<Feature>(8);
            feats.add(new Feature(FEATURE_NAMES[0], r.getClose()));
            feats.add(new Feature(FEATURE_NAMES[1], r.getLow()));
            feats.add(new Feature(FEATURE_NAMES[2], r.getHigh()));
            feats.add(new Feature(FEATURE_NAMES[3], r.getVolume()));
            feats.add(new Feature(FEATURE_NAMES[4], r.getEma7()));
            feats.add(new Feature(FEATURE_NAMES[5], r.getEma30()));
            feats.add(new Feature(FEATURE_NAMES[6], r.getEma99()));
            feats.add(new Feature(FEATURE_NAMES[7], r.getSsl()));
            dataset.add(new ListExample<Label>(y, feats));
        }
        return dataset;
    }
    
    /**
     * 4클래스 라벨:
     *  - UP_ONLY  : horizon 안에 +threshold 터치 O, -threshold 터치 X
     *  - DOWN_ONLY: horizon 안에 -threshold 터치 O, +threshold 터치 X
     *  - NONE     : 둘 다 X
     *  - BOTH     : 둘 다 O
     */
    public String label(List<FeatureRow> rows, int t, int horizonBars, double threshold) {

        double base = rows.get(t).getClose();
        double upPrice = base * (1.0 + threshold);
        double downPrice = base * (1.0 - threshold);

        boolean hitUp = false;
        boolean hitDown = false;

        int end = Math.min(t + horizonBars, rows.size() - 1);

        //boolean hitUp = false, hitDown = false;
        for (int i = t+1; i <= end; i++) {
            double c = rows.get(i).getClose();
            if (c >= upPrice) hitUp = true;
            if (c <= downPrice) hitDown = true;
            if(hitUp && hitDown) break;
        }

        if (hitUp && hitDown) return "BOTH";
        if (hitUp) return "UP_ONLY";
        if (hitDown) return "DOWN_ONLY";
        return "NONE";
    }
    
    
}

