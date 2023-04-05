package ai.enpasos.muzero.platform.agent.c_planning;

import ai.enpasos.muzero.platform.common.MuZeroException;
import lombok.Builder;
import lombok.Data;

import static ai.enpasos.muzero.platform.agent.c_planning.SequentialHalving.extraPhaseVisitsToUpdateQPerPhase;
import static ai.enpasos.muzero.platform.common.Functions.log2;

@Data
@Builder
public class SequentialHalving {

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
     * @param n number of simulations
     * @param m number of actions in the first phase - if larger than k it is changed to k
     */
    public static SequentialHalving initGumbelInfo(int n, int m, int k) {


       // m -= m % 2;   // make m a power of 2
        if (k >= 2) {  // make m smaller or equals k
            if (m>k) {
                m = k;
            }

        } else {
            throw new MuZeroException("k < 2 needs to be handled");
        }
        int phases = (int)Math.ceil(log2(m));
        return SequentialHalving.builder()
            .k(k)
            .n(n)
            .remainingBudget(n)
            .m(m)
            .phaseNum(phases)
            .extraVisitsPerPhase(extraPhaseVisitsToUpdateQPerPhase(n, m, phases))
            .build();

    }

    public static int[] extraPhaseVisitsToUpdateQPerPhase(int budget, int m) {
        return extraPhaseVisitsToUpdateQPerPhase(budget, m,(int) Math.ceil(log2(m)));
    }

    public static int[] extraPhaseVisitsToUpdateQPerPhase(int budget, int m, int phases) {
        int remainingBudget = budget;

        int[] result = new int[phases];
        for (int p = 0; p < phases; p++) {
            int na = (int) Math.floor((double) budget / phases / m);
            na = Math.max(1, na);
            if (p < phases - 1) {
                result[p] = na;
                remainingBudget -= na * m;
            } else {
                result[p] = Math.max(1, remainingBudget / m);
            }
            m /= 2;
        }
        return result;
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
