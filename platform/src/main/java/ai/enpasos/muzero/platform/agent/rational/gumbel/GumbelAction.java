package ai.enpasos.muzero.platform.agent.rational.gumbel;

import lombok.Builder;
import lombok.Data;

import static ai.enpasos.muzero.platform.agent.rational.gumbel.Gumbel.drawGumble;
import static ai.enpasos.muzero.platform.agent.rational.gumbel.Gumbel.logit;

@Data
@Builder
public class GumbelAction {
    int actionIndex;
    double policyValue;
    double gumbelValue;
    double logit;
    double q;
    int visitCount;

    public void initGumbelValueAndLogit() {
        gumbelValue = drawGumble();
        logit = logit(policyValue);
    }

}
