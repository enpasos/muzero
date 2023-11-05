package ai.enpasos.muzero.platform.agent.c_planning;

import ai.enpasos.muzero.platform.common.Functions;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GumbelFunctions {

    private GumbelFunctions() {
    }


    public static List<GumbelAction> drawGumbelActions(List<GumbelAction> gumbelActions, int n) {
        int[] actions = gumbelActions.stream().mapToInt(GumbelAction::getActionIndex).toArray();
        double[] g = gumbelActions.stream().mapToDouble(GumbelAction::getGumbelValue).toArray();
        double[] logits = gumbelActions.stream().mapToDouble(GumbelAction::getLogit).toArray();
        List<Integer> selectedActions = drawActions(actions, Functions.add(logits, g), n);
        return gumbelActions.stream().filter(a -> selectedActions.contains(a.actionIndex)).collect(Collectors.toList());
    }



    public static List<Integer> drawActions(int[] actions, double[] x, int n) {
        List<Integer> result = new ArrayList<>();

        List<Pair<Integer, Double>> gPlusLogits = IntStream.range(0, x.length).mapToObj(
            i -> new Pair<>(i, x[i])
        ).collect(Collectors.toList());

        IntStream.range(0, n).forEach(i -> {
            Pair<Integer, Double> selected = gPlusLogits.stream().max(Comparator.comparingDouble(Pair::getValue)).get();

            result.add(actions[selected.getKey()]);
            gPlusLogits.remove(selected);
        });

        return result;
    }


    static double drawGumble() {
        double r = 0d;
        while (r == 0d) {
            r = ThreadLocalRandom.current().nextDouble();
        }
        return -Math.log(-Math.log(r));
    }

    public static double[] sigmas(double[] qs, double maxActionVisitCount, int cVisit, double cScale) {
        return Arrays.stream(qs).map(q -> (cVisit + maxActionVisitCount) * cScale * q).toArray();
    }

}
