package ai.enpasos.muzero.platform.agent.rational;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.drawGumble;


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
    int visitCount;

    public double getQValue() {
        if (node != null) {
            return node.getQValue();
        }
        return qValue;
    }

    public double getLogit() {
        if (node != null) {
            return node.getLogit();
        }
        return logit;
    }

    public void initGumbelValue() {
        gumbelValue = drawGumble();
    }

}
