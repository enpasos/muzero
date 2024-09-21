package ai.enpasos.muzero.platform.agent.e_experience.box;

import ai.djl.util.Pair;

import java.util.*;


public class Boxes {



    /**
     * Checks if any of the boxes are relevant based on the list of relevant boxes.
     *
     * @param boxesRelevant a list of relevant box values
     * @param boxes         an array of box values
     * @return true if any box in 'boxes' is contained in 'boxesRelevant'; false otherwise
     */
    public static boolean hasRelevantBox(List<Integer> boxesRelevant, int[] boxes, int unrollSteps) {
        if (boxes == null || boxesRelevant == null) {
            return false;
        }

        // Convert the list to a set for faster lookup
        Set<Integer> relevantBoxesSet = new HashSet<>(boxesRelevant);

        for (int u = 0; u < unrollSteps && u < boxes.length; u++) {
            int box = boxes[u];
            if (relevantBoxesSet.contains(box)) {
                return true;
            }
        }
        return false;
    }




    /**
     * Updates the boxes array based on the provided parameters.
     *
     * @param boxes         the original boxes array
     * @param uok           the uok value
     * @param uOkClosed     whether uOk is closed
     * @param uOkTested     whether uOk is tested
     * @param boxesRelevant list of relevant box values
     * @return a Pair containing a boolean indicating if the boxes array was changed and the updated boxes array
     */
    public static Pair<Boolean, int[]> updateBoxes(int[] boxes, int uok, boolean uOkClosed, boolean uOkTested, List<Integer> boxesRelevant) {
        // Determine the target length of the boxes array and the index threshold
        int targetLength = Math.max(1, uOkClosed ? uok : uok + 1);
        int indexThreshold = uOkClosed ? targetLength : targetLength - 1;

        boolean changed = false;

        // Create a new array to avoid modifying the input array
        int[] updatedBoxes = new int[targetLength];
        System.arraycopy(boxes, 0, updatedBoxes, 0, Math.min(boxes.length, targetLength));
        if (boxes.length != targetLength) {
            changed = true;
        }

        // Update the boxes based on the conditions
        for (int i = 0; i < updatedBoxes.length; i++) {
            if (!uOkClosed && i >= indexThreshold) {
                if (updatedBoxes[i] != 0) {
                    updatedBoxes[i] = 0;
                    changed = true;
                }
            } else {
                boolean shouldIncrement = (uOkTested && boxesRelevant.contains(updatedBoxes[i])) || updatedBoxes[i] == 0;
                if (shouldIncrement) {
                    updatedBoxes[i]++;
                    changed = true;
                }
            }
        }

        return new Pair<>(changed, updatedBoxes);
    }

    /**
     * Finds the index of the smallest empty box in the array.
     * An empty box is represented by a zero value.
     *
     * @param boxes the array of box values
     * @return the index of the first empty box;
     *         returns the length of the array if no empty box is found;
     *         returns 0 if the input array is null or empty
     */
    public static int getSmallestEmptyBox(int[] boxes) {
        if (boxes == null || boxes.length == 0) {
            return 0;
        }
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i] == 0) {
                return i;
            }
        }
        return boxes.length;
    }



    /**
     * Retrieves the box value from the boxes array based on the given unroll steps.
     *
     * @param boxes       the array of box values
     * @param unrollSteps the number of unroll steps (1-based index)
     * @return  the box value at the given unroll steps;
     * @throws IllegalArgumentException if unrollSteps is negative
     */
    public static int getBox(int[] boxes, int unrollSteps) {
        if (boxes == null || boxes.length == 0) {
            return 0;
        }
        if (unrollSteps <= 0) {
            return 0;
        }
        if (unrollSteps > boxes.length) {
            return 0;
        }
        return boxes[unrollSteps - 1];
    }



}
