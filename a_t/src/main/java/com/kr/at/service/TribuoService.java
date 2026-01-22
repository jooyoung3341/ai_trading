package com.kr.at.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;

import com.kr.at.common.Common;
import com.kr.at.common.Indicator;
import com.kr.at.model.FeatureRow;
import com.kr.at.model.FeatureRowPct;

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

	@Value("${model.path}")
	private String model_path;
	
	@Autowired
	private BinanceService bService;
	@Autowired
	private Indicator indicator;
	@Autowired
	private Common common;
	
	public enum Y { UP_ONLY, DOWN_ONLY, NONE, UP_FIRST, DOWN_FIRST}
		
	
    // 피처 이름(이름표??)
    public static final String[] FEATURE_NAMES = {
            "lowPct", "highPct", "volLog", "ema7Pct", "ema30Pct", "ema99Pct", "sslPct"
    };
    
    
    public Model<Label> loadModel(String modelName) throws Exception {
        Path dir = Path.of(model_path);
        String fileName = modelName.endsWith(".model") ? modelName : modelName + ".model";
        Path modelPath = dir.resolve(fileName);
        // 저장했던 model 파일을 읽음
        return (Model<Label>) Model.deserializeFromFile(modelPath);
    }

    //예측
    public Prediction<Label> predict(Model<Label> model, FeatureRow fr) {
        System.out.println("=== [PREDICT INPUT] start =========================");
        System.out.println("input FeatureRow = " + fr);
        System.out.println("=== [PREDICT INPUT] end =========================");
        
        double close  = fr.getClose();
        double vol    = fr.getVolume();
        double ema7   = fr.getEma7();
        double ema30  = fr.getEma30();
        double ema99  = fr.getEma99();
        double ssl    = fr.getSsl();

        System.out.printf("close=%.8f vol=%.8f ema7=%.8f ema30=%.8f ema99=%.8f ssl=%.8f%n",
                close, vol, ema7, ema30, ema99, ssl);

        // 이상치 체크
        if (!Double.isFinite(close) || !Double.isFinite(vol) || !Double.isFinite(ema7) ||
            !Double.isFinite(ema30) || !Double.isFinite(ema99) || !Double.isFinite(ssl)) {
            System.out.println("[PREDICT - WARN] non-finite feature exists!");
        }
        if (close <= 0) System.out.println("[PREDICT - WARN] close <= 0");
        if (vol < 0)    System.out.println("[PREDICT - WARN] volume < 0");

        //테스트용 로그=======
        //var fmap = model.getFeatureIDMap();
        //System.out.println("model features = " + fmap.keySet());
        //테스트용 로그=======
        
    	double volLog = (!Double.isFinite(fr.getVolume()) || fr.getVolume() < 0)
                ? 0.0
                : Math.log1p(fr.getVolume());
        List<Feature> feats = new ArrayList<>(7);
        feats.add(new Feature(FEATURE_NAMES[0], common.pctFrom(close, fr.getLow())));
        feats.add(new Feature(FEATURE_NAMES[1], common.pctFrom(close, fr.getHigh())));
        feats.add(new Feature(FEATURE_NAMES[2], volLog));
        feats.add(new Feature(FEATURE_NAMES[3], common.pctFrom(close, fr.getEma7())));
        feats.add(new Feature(FEATURE_NAMES[4], common.pctFrom(close, fr.getEma30())));
        feats.add(new Feature(FEATURE_NAMES[5], common.pctFrom(close, fr.getEma99())));
        feats.add(new Feature(FEATURE_NAMES[6], common.pctFrom(close, fr.getSsl())));

        //Example<T> : 한 건의 학습/예측 샘플
        Example<Label> ex = new ListExample<>(new Label(Label.UNKNOWN), feats);

        //예측 실행
        return model.predict(ex);
    }

    //예측 결과
    public Map<String, Object> printResult(Prediction<Label> pred) {
        //String predicted = pred.getOutput().getLabel(); // Label의 이름 꺼내기 :contentReference[oaicite:2]{index=2}
        //System.out.println("predicted=" + predicted);
        Map<String, Label> scores = pred.getOutputScores();
        
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("UP_ONLY", scores.containsKey("UP_ONLY")   ? scores.get("UP_ONLY").getScore()*100   : 0.0);
        resultMap.put("DOWN_ONLY", scores.containsKey("DOWN_ONLY")   ? scores.get("DOWN_ONLY").getScore()*100   : 0.0);
        resultMap.put("NONE", scores.containsKey("NONE")   ? scores.get("NONE").getScore()*100   : 0.0);
        resultMap.put("UP_FIRST", scores.containsKey("UP_FIRST")   ? scores.get("UP_FIRST").getScore()*100  : 0.0);
        resultMap.put("DOWN_FIRST", scores.containsKey("DOWN_FIRST")   ? scores.get("DOWN_FIRST").getScore()*100   : 0.0);
        
        System.out.println("=== [PREDICT - RESULT - OUTPUT] ========================");
        System.out.println("predicted=" + pred.getOutput().getLabel());
        scores.entrySet().stream()
                .sorted((a,b) -> Double.compare(b.getValue().getScore(), a.getValue().getScore()))
                .forEach(e -> System.out.println(e.getKey() + " : " + e.getValue().getScore()*100));
        
        return resultMap;
    }
    
    
    
    /**
     *  전체 흐름:
     * featureRows(이미 지표 계산된 원본) → 라벨 생성 → TrainingRow 리스트 → Dataset → train → save
     */
    // horizonBars 예: 5분봉 1시간=12
    // threshold 예: 0.003 (0.3%)
    // warmupBars 예: EMA99 안정화로 200
    // modelPath 저장 경로
    public Path trainAndSaveTouch4WayModel(List<FeatureRow> featureRows, int horizonBars, double threshold, String modelName) throws IOException {
    	horizonBars++;
    	if (featureRows == null || featureRows.isEmpty()) {
            throw new IllegalArgumentException("featureRows is empty");
        }
        if (featureRows.size() <= horizonBars) {
            throw new IllegalArgumentException("Not enough rows. size=" + featureRows.size()
                   +", horizonBars=" + horizonBars);
        }

        for (int k = 0; k < Math.min(5, featureRows.size()); k++) {
            FeatureRow r = featureRows.get(k);
        }
        
        // 1) 라벨 생성해서 TrainingRow 만들기 (정답지 만드는 단계)
       // Touch4WayLabeler labeler = new Touch4WayLabeler();
        List<FeatureRowPct> featureRowsPct = new ArrayList<FeatureRowPct>(featureRows.size());

        for (int t = 0; t < featureRows.size(); t++) {
            if (t + horizonBars >= featureRows.size()) break; // 미래를 봐야 라벨 가능

            FeatureRow fr = featureRows.get(t);
            double c = fr.getClose();
            String y = label(featureRows, t, horizonBars, threshold);
            
            double highPct  = common.pctFrom(c, fr.getHigh());  // (high/close)-1
            double lowPct   = common.pctFrom(c, fr.getLow());   // (low/close)-1

            double ema7Pct  = common.pctFrom(c, fr.getEma7());
            double ema30Pct = common.pctFrom(c, fr.getEma30());
            double ema99Pct = common.pctFrom(c, fr.getEma99());
            double sslPct   = common.pctFrom(c, fr.getSsl());
            
            // 거래량은 %로 만들기보단 log1p가 보통 더 안정적
            double volLog = (!Double.isFinite(fr.getVolume()) || fr.getVolume() < 0)
                    ? 0.0
                    : Math.log1p(fr.getVolume());
            
            featureRowsPct.add(new FeatureRowPct(
            		lowPct, highPct, volLog,
                    ema7Pct, ema30Pct, ema99Pct, sslPct, 
                    y
            ));
        }

        // 분포 로그 (Dataset 만들기 전)
        int up=0, down=0, none=0, both=0, other=0;
        for (FeatureRowPct r : featureRowsPct) {
            String y = r.getLabel();
            if ("UP_ONLY".equals(y)) up++;
            else if ("DOWN_ONLY".equals(y)) down++;
            else if ("NONE".equals(y)) none++;
            else if ("BOTH".equals(y)) both++;
            else other++;
        }
        System.out.println("[LABEL_DIST] total=" + featureRows.size()
                + " UP_ONLY=" + up + " DOWN_ONLY=" + down + " NONE=" + none + " BOTH=" + both + " other=" + other);
        
        // 2) Dataset 생성(Tribuo가 학습 가능한 형태로 변환)
        MutableDataset<Label> dataset = buildDatasetPct(featureRowsPct, "touch4way_H" + horizonBars + "_T" + threshold);

        //Dataset 만든 직후
        //TransformationMap tmap = new TransformationMap(
         //   java.util.List.of(new MeanStdDevTransformation()) // 평균0/표준편차1 :contentReference[oaicite:4]{index=4}
        //);

        // 3) 학습 (MVP: Logistic Regression)
        //학습 객체
        LogisticRegressionTrainer trainer = new LogisticRegressionTrainer();
        //학습 수행
        Model<Label> model = trainer.train(dataset);
        
        Path dir = Path.of(model_path);
        String fileName = modelName.endsWith(".model") ? modelName : modelName + ".model";
        Path modelPath = dir.resolve(fileName);
        
        // 4) 저장
        Files.createDirectories(modelPath.getParent());
        model.serializeToFile(modelPath);

        return modelPath;
    }

    private MutableDataset<Label> buildDatasetPct(List<FeatureRowPct> rows, String datasourceId) {
        LabelFactory labelFactory = new LabelFactory();

        SimpleDataSourceProvenance prov =
                new SimpleDataSourceProvenance(datasourceId, OffsetDateTime.now(), labelFactory);
        MutableDataset<Label> dataset = new MutableDataset<Label>(prov, labelFactory);
        
        for (FeatureRowPct r : rows) {
            Label output = labelFactory.generateOutput(r.getLabel());
            List<Feature> feats = new ArrayList<Feature>(7);
            feats.add(new Feature(FEATURE_NAMES[0], r.getLowPct()));
            feats.add(new Feature(FEATURE_NAMES[1], r.getHighPct()));
            feats.add(new Feature(FEATURE_NAMES[2], r.getVolLog()));
            feats.add(new Feature(FEATURE_NAMES[3], r.getEma7Pct()));
            feats.add(new Feature(FEATURE_NAMES[4], r.getEma30Pct()));
            feats.add(new Feature(FEATURE_NAMES[5], r.getEma99Pct()));
            feats.add(new Feature(FEATURE_NAMES[6], r.getSslPct()));

            dataset.add(new ListExample<Label>(output, feats));
        }

        return dataset;
    }
    
    /**
     * 6클래스 라벨:
     *  - UP_ONLY  : horizon 안에 +threshold 터치 O, -threshold 터치 X
     *  - DOWN_ONLY: horizon 안에 -threshold 터치 O, +threshold 터치 X
     *  - NONE     : 둘 다 X
     */
    public String label(List<FeatureRow> rows, int t, int horizonBars, double threshold) {

    	//종가
        double base = rows.get(t).getClose();
        double upPrice = base * (1.0 + threshold);
        double downPrice = base * (1.0 - threshold);

        //봉 몇개를 볼 지 정함
        int end = Math.min(t + horizonBars, rows.size() - 1);

        int firstUpIdx = -1;
        int firstDownIdx = -1;
        
        for (int i = t + 1; i <= end; i++) {
            FeatureRow f = rows.get(i);

            // 아직 기록 안 됐으면 "처음 터치한 시점" 저장
            if (firstUpIdx == -1 && f.getHigh() >= upPrice) {
                firstUpIdx = i;
            }
            if (firstDownIdx == -1 && f.getLow() <= downPrice) {
                firstDownIdx = i;
            }

            // 둘 다 최초 시점이 잡혔으면 더 볼 필요 없음
            if (firstUpIdx != -1 && firstDownIdx != -1) break;
        }
        
        boolean hitUp = firstUpIdx != -1;
        boolean hitDown = firstDownIdx != -1;
        
        if (hitUp && hitDown) {
            // 둘 다 터치한 경우: 먼저 터치한 쪽을 라벨로
            // (같은 봉에서 둘 다 터치하면 "먼저"를 알 수 없으니 규칙으로 처리)
            if (firstUpIdx <= firstDownIdx) return Y.UP_FIRST.name();
            else return Y.DOWN_FIRST.name();
        }

        if (hitUp) return Y.UP_ONLY.name();
        if (hitDown) return Y.DOWN_ONLY.name();
        return Y.NONE.name();
    }
    
    
}

