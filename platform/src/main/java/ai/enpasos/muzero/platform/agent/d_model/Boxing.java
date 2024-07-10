package ai.enpasos.muzero.platform.agent.d_model;

import java.util.ArrayList;
import java.util.List;

public class Boxing {



    public static int intervall(int box) {
        return (int)Math.pow(2, box);
    }

    public static boolean isUsed(int box, int epoch) {
        return epoch % intervall(box) == 0;
    }


    public static List<Integer> boxesRelevant(int epoch, int maxBox) {
        List<Integer> boxesRelevant = new ArrayList<>();
        for (int b = 0; b <= maxBox; b++) {
            if(isUsed(b, epoch)) {
                boxesRelevant.add(b);
            }
        }
        return boxesRelevant;
    }
    public static List<Integer> boxesRelevantAndOccupied(List<Integer> occupiedBoxes, int epoch, int maxBox) {
        List<Integer> boxesRelevant = new ArrayList<>();
        int localEpoch = epoch - 1;
        while (occupiedBoxes.isEmpty()) {
            localEpoch++;
            for (int b = 0; b <= maxBox; b++) {
                if (isUsed(b,  localEpoch) && occupiedBoxes.contains(b)) {
                    boxesRelevant.add(b);
                }
            }
        }
        return boxesRelevant;
    }

//    public static List<Integer> boxesRelevant2(int epoch, int maxBox, int unrollSteps) {
//        if (unrollSteps == 1) return boxesRelevant(epoch, maxBox);
//        return List.of(0);
//    }
}
