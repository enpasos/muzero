package ai.enpasos.muzero.gamebuffer.modern;

import ai.enpasos.muzero.environments.OneOfTwoPlayer;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.gamebuffer.GameDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.TreeMap;

@Data
public class StateNode {
    Integer causingAction;
    Game game;
    StateNode parent;
    Map<Integer, StateNode> children = new TreeMap<>();

    public StateNode(StateNode parent, Integer causingAction) {
        this.parent = parent;
        this.causingAction = causingAction;
    }

    public StateNode getNode(Integer causingAction) {
        if (!children.containsKey(causingAction)) {
            children.put(causingAction, new StateNode(this, causingAction));
        }
        return children.get(causingAction);
    }

    public boolean hasOrIsLeafNodeWithPositivResult(OneOfTwoPlayer player) {
        if (children.isEmpty()) {
            return !this.game.getEnvironment().hasPlayerWon(OneOfTwoPlayer.otherPlayer(player));
        } else {
            for (StateNode child: children.values()) {
                if (child.hasOrIsLeafNodeWithPositivResult(player)) return true;
            }
            return false;
        }
    }
}
