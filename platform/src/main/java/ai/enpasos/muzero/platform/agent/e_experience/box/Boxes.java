package ai.enpasos.muzero.platform.agent.e_experience.box;

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


    public static boolean toUOk(int[] boxes, int uok, boolean uOkClosed, boolean uOkTested) {
        int targetUOK = Math.max(1, uok + 1);
        int boxIndex = targetUOK - 1;
        boolean changed = false;
        for(int b = 0; b < boxes.length; b++) {

            if (b >= boxIndex && !uOkClosed) {
                if (boxes[b] != 0) {
                    boxes[b] = 0;
                    changed = true;
                }
            } else {

                if (uOkTested || boxes[b] <= 0 ) {
                    boxes[b]++;
                    changed = true;
                }
            }
        }
        return changed;
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
}
