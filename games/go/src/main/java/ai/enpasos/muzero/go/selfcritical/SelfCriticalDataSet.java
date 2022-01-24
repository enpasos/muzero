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
////        double minEntropy = features.stream()
////            .mapToDouble(f -> f.entropy)
////            .reduce(0, Double::min);
////        double maxEntropy = features.stream()
////            .mapToDouble(f -> f.entropy)
////            .reduce(0, Double::max);

        features.stream().forEach(f -> f.entropy = f.entropy / maxEntropy);


            maxMoveNumber = features.stream()
            .mapToInt(f -> f.numberOfMovesPlayedSofar)
            .reduce(0, Integer::max);
        features.stream().forEach(f -> f.numberOfMovesPlayedSofar = f.numberOfMovesPlayedSofar / maxMoveNumber);
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
        // int n = splitTrainingTestNumbers().getValue();
        SelfCriticalDataSet newDataSet = new SelfCriticalDataSet();
        newDataSet.setFeatures(features.subList(numbers.getValue(), numbers.getValue()+ numbers.getKey()));
        newDataSet.setMaxEntropy(this.maxEntropy);
        newDataSet.setMaxMoveNumber(this.getMaxMoveNumber());
        return newDataSet;
    }

    public Pair<Integer, Integer> splitTrainingTestNumbers() {
        int l = this.getFeatures().size();
        int lTrain = (int)(l*0.8);
        int lTest = l - lTrain;
        return new Pair(lTrain,lTest );
    }


}
