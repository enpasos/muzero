package ai.enpasos.muzero.platform.agent.e_experience.box;

import java.util.ArrayList;
import java.util.List;

public class Boxing {

    public static final int MAX_BOX = 5;

    public static int intervall(int box) {
        return (int)Math.pow(2, box);
    }

    public static boolean isUsed(int box, int epoch) {
        return epoch % intervall(box) == 0;
    }


    public static List<Integer> boxesRelevant(int epoch,   boolean training) {
        List<Integer> boxesRelevant = new ArrayList<>();
        for (int b = 0; b <= (training ? MAX_BOX-1 : MAX_BOX) ; b++) {
            if (  isUsed(b, epoch)) {
                    boxesRelevant.add(b);
                }

        }
        return boxesRelevant;
    }

}
