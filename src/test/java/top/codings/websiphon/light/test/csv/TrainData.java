package top.codings.websiphon.light.test.csv;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TrainData {
    private String emotion;
    private String content;
    private String prediction;

    public TrainData(String emotion, String content) {
        this.emotion = emotion;
        this.content = content;
    }
}
