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

   private Integer[] boxes;

   private Integer uOk;
   private Integer nextUOk;
   private Integer nextUOkTarget;



   private Integer t;


   private boolean uOkClosed;





   public boolean isTrainable() {
      return (nextUOk >= nextUOkTarget || uOk < 1);
   }

   public int getSmallestEmptyBox() {
        return Boxes.getSmallestEmptyBox(boxes);
   }
}
