package ai.enpasos.muzero.platform.agent.b_planning;

import ai.enpasos.muzero.platform.common.MuZeroException;
import lombok.Builder;
import lombok.Data;

import static ai.enpasos.muzero.platform.agent.b_planning.GumbelFunctions.extraPhaseVisitsToUpdateQPerPhase;
import static ai.enpasos.muzero.platform.common.Functions.log2;

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
    int phaseNum;
    int[] extraVisitsPerPhase;

    int remainingBudget;


    @Builder.Default
    boolean finished = false;


    /**
     * @param n number of simulations, this value could be dynamic
     * @param m number of actions in the first phase - is changed to the next smaller powers of 2, that is smaller or equals k
     */
    public static GumbelInfo initGumbelInfo(int n, int m, int k) {


        m -= m % 2;   // make m a power of 2
        if (k >= 2) {  // make m smaller or equals k
            while (m > k) {
                m /= 2;
            }
        } else {
            throw new MuZeroException("k < 2 needs to be handled");
        }
        return GumbelInfo.builder()
            .k(k)
            .n(n)
            .remainingBudget(n)
            .m(m)
            .phaseNum((int) log2(m))
            .extraVisitsPerPhase(extraPhaseVisitsToUpdateQPerPhase(n, m))
            .build();

    }


    public void next() {
        remainingBudget--;
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
        if (remainingBudget == 0) {
            finished = true;
        }
        if (finished) {
            m = 1;
        }
    }


}
