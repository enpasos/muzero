package ai.enpasos.muzero.platform.agent.rational.gumbel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GumbelInfo {

    @Builder.Default
    boolean phaseChanged = false;
    int m;  // is changing
    int im;
    int phase;
    int extraVisit;
    int n;
    int k;
    int[] extraVisitsPerPhase;


    @Builder.Default
    boolean finished = false;



    public void next() {
        if (finished && m == 1) return;
        if (phase == this.extraVisitsPerPhase.length - 1
            && this.extraVisit == this.extraVisitsPerPhase[this.extraVisitsPerPhase.length - 1] - 1
            && im == m - 1) {
            finished = true;
        }
        phaseChanged = false;
        if (im < m - 1) {
            im++;
            onFinishedM2One();
            return;
        } else {
            im = 0;
        }
        if (extraVisit < extraVisitsPerPhase[phase] - 1) {
            extraVisit++;
            onFinishedM2One();
            return;
        } else {
            extraVisit = 0;
        }
        if (phase < extraVisitsPerPhase.length - 1) {
            phase++;
            phaseChanged = true;
            m /= 2;
            onFinishedM2One();
            return;
        }
        onFinishedM2One();
    }

    private void onFinishedM2One() {
        if (finished) {
            m = 1;
        }
    }


}
