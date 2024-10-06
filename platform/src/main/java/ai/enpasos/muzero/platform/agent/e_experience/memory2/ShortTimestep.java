package ai.enpasos.muzero.platform.agent.e_experience.memory2;

import ai.enpasos.muzero.platform.agent.e_experience.box.Boxes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShortTimestep {
   @EqualsAndHashCode.Include
   private Long id;

   private Long episodeId;

   private int[] boxes;

   public ShortTimestep(Long id, Long episodeId, int[] boxes, Integer uOk, Integer nextUOk, boolean nextuokclosed, Integer t, boolean uOkClosed, int uOkEpoch) {
      this.id = id;
      this.episodeId = episodeId;
      this.boxes = boxes;
      this.uOk = uOk;
      this.nextUOk = nextUOk;
      this.nextuokclosed = nextuokclosed;
      this.t = t;
      this.uOkClosed = uOkClosed;
      this.uOkEpoch = uOkEpoch;
   }

   public ShortTimestep(Long id, Long episodeId, int[] boxes, Integer uOk, Integer nextUOk, boolean nextuokclosed, Integer t, boolean uOkClosed, boolean justTrained, int uOkEpoch) {
      this.id = id;
      this.episodeId = episodeId;
      this.boxes = boxes;
      this.uOk = uOk;
      this.nextUOk = nextUOk;
      this.nextuokclosed = nextuokclosed;
      this.t = t;
      this.uOkClosed = uOkClosed;
      this.uOkEpoch = uOkEpoch;
      this.justTrained = justTrained;
   }

   public ShortTimestep() {
   }

   public static ShortTimestepBuilder builder() {
      return new ShortTimestepBuilder();
   }

   public int getLastBox() {
      return boxes[boxes.length - 1];
   }

   private Integer uOk;
   private Integer nextUOk;
   private boolean nextuokclosed;


   private Integer t;


   private boolean uOkClosed;


   private boolean justTrained;


   private int uOkEpoch;


//   public boolean hasToBeTrained(int unrollSteps, Map<Long, Integer> episodeIdToMaxTime) {
//      int tmax = episodeIdToMaxTime.get(getEpisodeId());
//      return hasToBeTrained(unrollSteps, tmax);
//
//   }

//   public boolean hasToBeTrained(int unrollSteps, int tmax) {
//      return isTrainable(unrollSteps, tmax) && needsTraining(unrollSteps);
//   }

//   public boolean isTrainable(int unrollSteps ) {
//
//      return uOk < unrollSteps;
//
//
//      // Check if unrollSteps is 1 and uOk is less than unrollSteps
//      boolean condition1 = (unrollSteps == 1 && uOk < unrollSteps);
//
//      // Check if t is greater than tMax - unrollSteps - 1
//      boolean condition2 = (t > tMax - unrollSteps - 1);
//
//      // Check if t is less than or equal to tMax - unrollSteps - 1 and uOk is less than unrollSteps - 1
//      boolean condition3 = (t <= tMax - unrollSteps - 1 && uOk < unrollSteps - 1);
//
//      // Return true if any of the conditions are true
//      return condition1 || condition2 || condition3;
//   }

   public boolean needsTraining(int unrollSteps) {
      return !uOkClosed && uOk < unrollSteps;
   }

   public boolean needsTrainingPrio1(int tmax, int unrollSteps) {
      int timeRemaining = tmax - t;
      return timeRemaining <= unrollSteps && needsTraining(unrollSteps);
   }

   public boolean needsTrainingPrio2(int tmax, int unrollSteps) {
      int timeRemaining = tmax - t;
      return timeRemaining > unrollSteps && needsTraining(unrollSteps);
   }

   public int getBox(int unrollSteps) {
      return Boxes.getBox(boxes, unrollSteps);
   }


   public Integer getUnrollSteps(int tmax, int unrollStepsEpisode) {

      int timeRemaining = tmax - t;
      int unrollSteps = Math.min(timeRemaining, unrollStepsEpisode);
      unrollSteps = Math.max(1, unrollSteps);

      return unrollSteps;
   }


}
