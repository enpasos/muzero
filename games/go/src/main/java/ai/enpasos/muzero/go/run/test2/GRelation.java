package ai.enpasos.muzero.go.run.test2;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GRelation {

    GNode from;
    GNode to;
    int action;


   public boolean isLeaf() {
        return to.outRelations.isEmpty();
   }

    public void attachToNodes() {
        from.outRelations.add(this);
        to.inRelations.add(this);
    }
}
