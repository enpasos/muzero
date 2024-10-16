package ai.enpasos.muzero.platform.agent.e_experience.memory2;

import ai.enpasos.muzero.platform.agent.e_experience.box.Boxes;
import ai.enpasos.muzero.platform.agent.e_experience.box.Boxing;
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

   private int[] boxesEpisodeEntered;

   public ShortTimestep(Long id, Long episodeId, int[] boxes, Integer uOk, Integer nextUOk, boolean nextuokclosed, Integer t, boolean uOkClosed, int uOkEpoch, int[] boxesEpisodeEntered) {
      this.id = id;
      this.episodeId = episodeId;
      this.boxes = boxes;
      this.uOk = uOk;
      this.nextUOk = nextUOk;
      this.nextuokclosed = nextuokclosed;
      this.t = t;
      this.uOkClosed = uOkClosed;
      this.uOkEpoch = uOkEpoch;
      this.boxesEpisodeEntered = boxesEpisodeEntered;
   }

   public ShortTimestep(Long id, Long episodeId, int[] boxes, Integer uOk, Integer nextUOk, boolean nextuokclosed, Integer t, boolean uOkClosed, boolean justTrained, int uOkEpoch, int[] boxesEpisodeEntered) {
      this.id = id;
      this.episodeId = episodeId;
      this.boxes = boxes;
      this.uOk = uOk;
      this.nextUOk = nextUOk;
      this.nextuokclosed = nextuokclosed;
      this.t = t;
      this.uOkClosed = uOkClosed;
      this.uOkEpoch = uOkEpoch;
      this.boxesEpisodeEntered = boxesEpisodeEntered;
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

   private int epochEnteredBox0;



   public boolean needsTraining(int unrollSteps) {
      return !uOkClosed && uOk < unrollSteps;
   }

//   public boolean isPrio1(int tmax, int unrollSteps) {
//      int timeRemaining = tmax - t;
//      return timeRemaining <= unrollSteps  ;
//   }
//
//   public boolean needsTrainingPrio1(int tmax, int unrollSteps) {
//      return isPrio1(tmax, unrollSteps) && needsTraining(unrollSteps);
//   }
//
//   public boolean needsTrainingPrio2(int tmax, int unrollSteps) {
//      return isPrio2(  tmax,  unrollSteps) && needsTraining(unrollSteps);
//   }
//   public boolean isPrio2(int tmax, int unrollSteps) {
//      int timeRemaining = tmax - t;
//      return timeRemaining > unrollSteps  ;
//   }

   public int getBox(int unrollSteps) {
      return Boxes.getBox(boxes, unrollSteps);
   }

   public int getEpisodeEnteredBox(int unrollSteps) {
      return Boxes.getBox(this.boxesEpisodeEntered, unrollSteps);
   }


   public Integer getUnrollSteps(int tmax, int unrollStepsEpisode) {

      int timeRemaining = tmax - t;
      int unrollSteps = Math.min(timeRemaining, unrollStepsEpisode);
      unrollSteps = Math.max(1, unrollSteps);

      return unrollSteps;
   }


   public boolean isEpochRelevant(int epoch, int unrollSteps) {
     return Boxing.isBoxRelevant(epoch,  getEpisodeEnteredBox(unrollSteps), getBox(unrollSteps));
   }
}
