package ai.enpasos.muzero.platform.agent.e_experience.box;

import ai.djl.util.Pair;

import java.util.List;
import java.util.StringJoiner;
import java.util.StringTokenizer;


public class Boxes {


//    public static String encode(int[] boxes) {
//        StringJoiner sj = new StringJoiner(",");
//        for (int box : boxes) {
//            sj.add(Integer.toString(box));
//        }
//        return sj.toString();
//    }
//
//    public static int[] decode(String boxes) {
//        StringTokenizer tokenizer = new StringTokenizer(boxes, ",");
//        int[] result = new int[tokenizer.countTokens()];
//        int i = 0;
//        while (tokenizer.hasMoreTokens()) {
//            result[i++] = Integer.parseInt(tokenizer.nextToken());
//        }
//        return result;
//    }


    public static boolean hasRelevantBox(List<Integer> boxesRelevant, int[] boxes) {
        for (int box : boxes) {
            if (boxesRelevant.contains(box)) {
                return true;
            }
        }
        return false;
    }


    public static Pair<Boolean, int[]> toUOk(int[] boxes, int uok, boolean uOkClosed, boolean uOkTested) {
        int targetUOK = Math.max(1, uok + 1);
        int boxIndex = targetUOK - 1;
        if (uOkClosed) {
            targetUOK = Math.max(1, uok);
            boxIndex = targetUOK;
        }
        boolean changed = false;
        int[] boxesResult = boxes;
        if (boxes.length != targetUOK) {
            changed = true;
            boxesResult = new int[targetUOK];
            System.arraycopy(boxes, 0, boxesResult, 0, Math.min(boxes.length, boxesResult.length));
        }
        boxes = boxesResult;

        for(int b = 0; b < boxes.length; b++) {
            if (b >= boxIndex && !uOkClosed) {
                if (boxes[b] != 0) {
                    boxes[b] = 0;
                    changed = true;
                }
            } else {
                if (uOkTested) { // || boxes[b] <= 0 ) {
                    boxes[b]++;
                    changed = true;
                }
            }
        }
        return new Pair(changed, boxes);
    }


    public static int getSmallestEmptyBox(int[] boxes) {
        if (boxes == null) return 0;
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i] == 0) {
                return i;
            }
        }
        return boxes.length;
    }

    public static int getBox(int[] boxes, int unrollSteps) {
        if (boxes == null) return 0;
        if (unrollSteps <= 0) return 0;
        if (unrollSteps > boxes.length) return boxes.length;
        return boxes[unrollSteps - 1];
    }


}
