package ai.enpasos.muzero.go.run.test2;

import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GNode {
    List<GRelation> outRelations = new ArrayList<>();
    List<GRelation> inRelations = new ArrayList<>();

    StateKey stateKey;

    boolean expanded;

    int value;

 //  public GNode() {}

    public GNode(StateKey stateKey) {
       this.stateKey = stateKey;
    }

    public int toPlay() {
        return this.getStateKey().toPlay;
    }


    public boolean isLeaf() {
        return outRelations.isEmpty();
    }

    public boolean isTerminal() {
        return expanded && isLeaf();
    }

    public boolean isReady(int perspective) {
        return isTerminal() || isMaxMinReached(perspective);
    }

    public boolean isMaxMinReached(int perspective) {
        return  value == (perspective > 0 ? 1 : -1) ;
    }

    public void backpropagate(int v, int perspective) {
            if ( toPlay() ==  perspective) {
                v = Math.max( getValue(), v);
            } else {
                v = Math.min(getValue(), v);
            }

        this.setValue(v);
        for (GRelation r :  getInRelations()) {
            GNode parent =  r.from;
            if (parent != null) {
                parent.backpropagate(-v,   perspective);
            }
        }

    }
}
