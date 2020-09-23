package top.codings.websiphon.light.test.csv;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.atomic.LongAdder;

@Getter
@Setter
@NoArgsConstructor
public class EmotionStat {
    private String emotion;
    private int tp;
    private int fp;
    private int tn;
    private int fn;
    private float p;
    private float r;
    private float f1;
    private LongAdder tpAdder = new LongAdder();
    private LongAdder fpAdder = new LongAdder();
    private LongAdder tnAdder = new LongAdder();
    private LongAdder fnAdder = new LongAdder();

    public EmotionStat(String emotion) {
        this.emotion = emotion;
    }

    public void finish() {
        tp = (int) tpAdder.sum();
        fp = (int) fpAdder.sum();
        tn = (int) tnAdder.sum();
        fn = (int) fnAdder.sum();
        p = tp * 1f / (tp + fp);
        r = tp * 1f / (tp + fn);
        f1 = 2f / (1f / p + 1f / r);
    }
}
