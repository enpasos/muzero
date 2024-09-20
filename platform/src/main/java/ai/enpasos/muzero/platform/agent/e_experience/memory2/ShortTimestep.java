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





   public boolean isTrainable(int unrollSteps, int tMax)  {
      return (unrollSteps == 1 && uOk < 1) || (t > tMax - unrollSteps - 1  || nextUOk >= unrollSteps - 1);

    //  return nextuokclosed || nextUOk >= unrollSteps - 1 || (unrollSteps == 1 && uOk < 1);

   }

   public boolean needsTraining(int unrollSteps) {
      //  return nextuokclosed || nextUOk >= unrollSteps - 1 || uOk < 1;
      return   !uOkClosed && uOk < unrollSteps;
   }

//   public int getSmallestEmptyBox() {
//        return Boxes.getSmallestEmptyBox(boxes);
//   }


   public int getBox(int unrollSteps) {
      return Boxes.getBox(boxes, unrollSteps);
   }
}
