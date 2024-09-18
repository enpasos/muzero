package ai.enpasos.muzero.platform.agent.e_experience.memory2;

import ai.enpasos.muzero.platform.agent.e_experience.box.Boxes;
import lombok.*;

@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
public class ShortTimestep {
   @EqualsAndHashCode.Include
   private Long id;

   private Long episodeId;

   private int[] boxes;

   private Integer uOk;
   private Integer nextUOk;
   //private Integer nextUOkTarget;
   private boolean nextuokclosed;



   private Integer t;


   private boolean uOkClosed;


   public boolean isTrainable(int unrollSteps, int tmax) {
      if (unrollSteps == 0) return true;
      return (unrollSteps == getUnrollSteps(tmax)) && (uOkClosed || (nextuokclosed && !uOkClosed));
   }

   public boolean isTrainableAndNeedsTraining(int unrollSteps, int tmax ) {
      if (unrollSteps == 0) return uOk < 0 && !uOkClosed;
       return nextuokclosed && !uOkClosed && (unrollSteps == getUnrollSteps(tmax))  ;
   }

   public boolean isTrainableAndNeedsTraining( ) {
      return (uOk < 0 && !uOkClosed) || (nextuokclosed && !uOkClosed);
   }

   public int getUnrollSteps(int maxTime) {
      if (uOk < 0) return 0;
      return maxTime - t;
   }

//   public boolean needsTraining(int unrollSteps) {
//      //  return nextuokclosed || nextUOk >= unrollSteps - 1 || uOk < 1;
//      return    uOk < unrollSteps ;
//   }

   public int getSmallestEmptyBox() {
        return Boxes.getSmallestEmptyBox(boxes);
   }


   public int getBox(int unrollSteps) {
      return Boxes.getBox(boxes, unrollSteps);
   }
}
