package ai.enpasos.muzero.go.selfcritical;

import ai.djl.util.Pair;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SelfCriticalDataSet {

    List<SelfCriticalLabeledFeature> features;
    double maxEntropy = Math.log(2);
    int maxMoveNumber;

    public SelfCriticalDataSet() {
        features = new ArrayList<>();
    }


    public void transformRawToNormalizedInput() {
      //  features.stream().forEach(f -> f.transformRawToPreNormalizedInput());

        features.stream().forEach(f -> f.entropy = f.entropy / maxEntropy);

        maxMoveNumber = features.stream()
            .mapToInt(f -> f.numberOfMovesPlayedSofar)
            .reduce(0, Integer::max);
        features.stream().forEach(f -> f.normalizedNumberOfMovesPlayedSofar = (float)f.numberOfMovesPlayedSofar / (float)maxMoveNumber);

       // features.stream().limit(100).forEach(f -> System.out.println(f.numberOfMovesPlayedSofar + "; " +f.value));
        //features.stream().limit(100).forEach(f -> System.out.println(f));
    }

    public SelfCriticalDataSet getTrainingDataSet() {
        int n = splitTrainingTestNumbers().getKey();
        SelfCriticalDataSet newDataSet = new SelfCriticalDataSet();
        newDataSet.setFeatures(features.subList(0, n));
        newDataSet.setMaxEntropy(this.maxEntropy);
        newDataSet.setMaxMoveNumber(this.getMaxMoveNumber());
        return newDataSet;
    }

    public SelfCriticalDataSet getTestDataSet() {
        Pair<Integer, Integer> numbers = splitTrainingTestNumbers();
        // int n = splitTrainingTestNumbers().getValueFromInitialInception();
        SelfCriticalDataSet newDataSet = new SelfCriticalDataSet();
        newDataSet.setFeatures(features.subList(numbers.getKey(), numbers.getValue() + numbers.getKey()));
        newDataSet.setMaxEntropy(this.maxEntropy);
        newDataSet.setMaxMoveNumber(this.getMaxMoveNumber());
        return newDataSet;
    }

    public Pair<Integer, Integer> splitTrainingTestNumbers() {
        int l = this.getFeatures().size();
        int lTrain = (int) (l * 0.8);
        int lTest = l - lTrain;
        return new Pair(lTrain, lTest);
    }


}
