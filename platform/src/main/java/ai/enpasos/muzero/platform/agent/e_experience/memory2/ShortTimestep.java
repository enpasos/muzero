package ai.enpasos.muzero.platform.agent.e_experience.memory2;

import ai.enpasos.muzero.platform.agent.e_experience.box.Boxes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShortTimestep {
   @EqualsAndHashCode.Include
   private Long id;

   private Long episodeId;

   private int[] boxes;

   public ShortTimestep(Long id, Long episodeId, int[] boxes, Integer uOk, Integer nextUOk, boolean nextuokclosed, Integer t, boolean uOkClosed ) {
      this.id = id;
      this.episodeId = episodeId;
      this.boxes = boxes;
      this.uOk = uOk;
      this.nextUOk = nextUOk;
      this.nextuokclosed = nextuokclosed;
      this.t = t;
      this.uOkClosed = uOkClosed;
   }

   public ShortTimestep(Long id, Long episodeId, int[] boxes, Integer uOk, Integer nextUOk, boolean nextuokclosed, Integer t, boolean uOkClosed, boolean justTrained) {
      this.id = id;
      this.episodeId = episodeId;
      this.boxes = boxes;
      this.uOk = uOk;
      this.nextUOk = nextUOk;
      this.nextuokclosed = nextuokclosed;
      this.t = t;
      this.uOkClosed = uOkClosed;
      this.justTrained = justTrained;
   }

   public ShortTimestep() {
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


   public boolean hasToBeTrained(int unrollSteps, Map<Long, Integer> episodeIdToMaxTime) {
      int tmax = episodeIdToMaxTime.get(getEpisodeId());
      return hasToBeTrained(unrollSteps, tmax);

   }

   public boolean hasToBeTrained(int unrollSteps, int tmax) {
      return isTrainable(unrollSteps, tmax) && needsTraining(unrollSteps);
   }

   public boolean isTrainable(int unrollSteps, int tMax) {
      return (unrollSteps == 1 && uOk < 1) || (t > tMax - unrollSteps - 1);
   }

   public boolean needsTraining(int unrollSteps) {
      return !uOkClosed && uOk < unrollSteps;
   }

   public int getBox(int unrollSteps) {
      return Boxes.getBox(boxes, unrollSteps);
   }


}
