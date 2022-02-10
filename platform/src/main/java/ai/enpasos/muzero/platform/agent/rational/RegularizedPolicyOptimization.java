package ai.enpasos.muzero.platform.agent.rational;

import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/*
  see paper http://proceedings.mlr.press/v119/grill20a.html
 */
@Component
public class RegularizedPolicyOptimization {


    @Autowired
    MuZeroConfig config;

    public List<Pair<Action, Double>> getDistributionInput(@NotNull Node node, MinMaxStats minMaxStats) {

        List<Map.Entry<Action, Node>> list = new ArrayList<>(node.getChildren().entrySet());
        List<Pair<Action, Double>> distributionInput;
        if (node.getVisitCount() != 0) {
            double multiplierLambda = multiplierLambda(node);

            double alphaMin = list.stream()
                    .mapToDouble(an -> {
                        Node child = an.getValue();
                        return child.valueScore(minMaxStats, config) + multiplierLambda * child.getPrior();
                    })
                    .max().orElseThrow(MuZeroException::new);
            double alphaMax = list.stream()
                    .mapToDouble(an -> {
                        Node child = an.getValue();
                        return child.valueScore(minMaxStats, config);
                    })
                    .max().orElseThrow(MuZeroException::new) + multiplierLambda;

            double alpha = calcAlpha(list, multiplierLambda, alphaMin, alphaMax, minMaxStats);


            distributionInput =
                    list.stream()
                            .map(e -> Pair.create(e.getKey(), optPolicy(multiplierLambda, alpha, e.getValue(), minMaxStats)))
                            .collect(Collectors.toList());

        } else {
            // if a node has never been visited during MCTS then the best estimation for the action selection to its child nodes
            // is still the one from the network stored as prior (on the root possibly with noise)

            double sum = list.stream()
                    .mapToDouble(e -> e.getValue().getPrior())
                    .sum();
            distributionInput =
                    list.stream()
                            .map(e -> Pair.create(e.getKey(), e.getValue().getPrior() / sum))
                            .collect(Collectors.toList());

        }
        return distributionInput;
    }

    private double calcAlpha(List<Map.Entry<Action, Node>> list, double multiplierLambda, double alphaMin, double alphaMax, MinMaxStats minMaxStats) {
        // dichotomic search
        double optPolicySum;
        double alpha;
        double epsilon = 0.000000001d;
        int c = 0;
        do {
            alpha = (alphaMax + alphaMin) / 2d;
            optPolicySum = optPolicySum(list, multiplierLambda, alpha, minMaxStats);

            if (optPolicySum > 1d) {
                alphaMin = alpha;
            } else {
                alphaMax = alpha;
            }
        } while (++c < 100 && FastMath.abs(optPolicySum - 1d) > epsilon);
        return alpha;
    }

    private double optPolicySum(List<Map.Entry<Action, Node>> list, double multiplierLambda, double alpha, MinMaxStats minMaxStats) {
        return list.stream()
                .mapToDouble(e -> {
                    Node child = e.getValue();
                    return optPolicy(multiplierLambda, alpha, child, minMaxStats);
                })
                .sum();
    }

    private double optPolicy(double multiplierLambda, double alpha, Node child, MinMaxStats minMaxStats) {
        double optPolicy;
        optPolicy = multiplierLambda * child.getPrior() / (alpha - child.valueScore(minMaxStats, config));
        return optPolicy;
    }

    // from "MCTS as regularized policy optimization", equation 4
    private double multiplierLambda(@NotNull Node parent) {
        return c(parent) * Math.sqrt(parent.getVisitCount()) / (parent.getVisitCount() + config.getActionSpaceSize());
    }

    private double c(@NotNull Node parent) {
        double pbC;
        pbC = Math.log((parent.getVisitCount() + config.getPbCBase() + 1d) / config.getPbCBase()) + config.getPbCInit();
        return pbC;
    }
}
