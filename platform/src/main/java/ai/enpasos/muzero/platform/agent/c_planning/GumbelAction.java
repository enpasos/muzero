package ai.enpasos.muzero.platform.agent.c_planning;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static ai.enpasos.muzero.platform.agent.c_planning.GumbelFunctions.drawGumble;


@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GumbelAction {
    Node node;
    @EqualsAndHashCode.Include
    int actionIndex;
    double policyValue;
    double gumbelValue;
    double logit;
    double qValue;
    double entropyValue;

    int visitCount;

    public double getQValue() {
        if (node != null) {
            return node.getQValue();
        }
        return qValue;
    }

    public double getEntropyQValue() {
        if (node != null) {
            return node.getEntropyQValue();
        }
        return entropyValue;
    }

    public double getLogit() {
        if (node != null) {
            return node.getLogit();
        }
        return logit;
    }

    public void initGumbelValue(boolean withRandomness) {
        if (withRandomness) {
            gumbelValue = drawGumble();
        } else {
            gumbelValue = 0;
        }
    }

}
