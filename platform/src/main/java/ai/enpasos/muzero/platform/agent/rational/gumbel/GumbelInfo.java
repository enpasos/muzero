package ai.enpasos.muzero.platform.agent.rational.gumbel;

import ai.enpasos.muzero.platform.common.MuZeroException;
import lombok.Builder;
import lombok.Data;

import static ai.enpasos.muzero.platform.agent.rational.gumbel.SequentialHalving.extraPhaseVisitsToUpdateQPerPhase;

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


    public static GumbelInfo initGumbelInfo(int n, int m, int k) {

        if (k >= 2) {
            while (m > k) {
                m /= 2;
            }
        } else {
            throw new MuZeroException("k < 2 needs to be handled");
        }
        return GumbelInfo.builder()
            .k(k)
            .n(n)
            .m(m)
            .extraVisitsPerPhase(extraPhaseVisitsToUpdateQPerPhase(n, m))
            .build();

    }


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
