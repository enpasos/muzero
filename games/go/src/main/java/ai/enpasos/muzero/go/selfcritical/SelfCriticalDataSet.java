package ai.enpasos.muzero.go.selfcritical;

import ai.djl.util.Pair;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SelfCriticalDataSet {

    int maxFullMoves;

    List<SelfCriticalGame> data = new ArrayList<>();

    public SelfCriticalDataSet getTrainingDataSet() {
        int n = splitTrainingTestNumbers().getKey();
        SelfCriticalDataSet newDataSet = new SelfCriticalDataSet();
        newDataSet.maxFullMoves = this.maxFullMoves;
        newDataSet.setData(data.subList(0, n));
        return newDataSet;
    }

    public SelfCriticalDataSet getTestDataSet() {
        Pair<Integer, Integer> numbers = splitTrainingTestNumbers();
        SelfCriticalDataSet newDataSet = new SelfCriticalDataSet();
        newDataSet.maxFullMoves = this.maxFullMoves;
        newDataSet.setData(data.subList(numbers.getKey(), numbers.getValue() + numbers.getKey()));
        return newDataSet;
    }

    public Pair<Integer, Integer> splitTrainingTestNumbers() {
        int l = this.getData().size();
        int lTrain = (int) (l * 0.95);
        int lTest = l - lTrain;
        return new Pair(lTrain, lTest);
    }


}
