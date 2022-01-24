package ai.enpasos.muzero.go.selfcritical;

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

}
