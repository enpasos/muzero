package ai.enpasos.muzero.gamebuffer.modern;

import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.gamebuffer.GameDTO;

import java.util.List;

public class GameTree {
    
    StateNode root;
    
    public GameTree() {
        root = new StateNode(null, null);
    }
    
    public void addGame(Game game ) {
        List<Integer> actions = game.getGameDTO().getActionHistory();
        StateNode node = root;
        for (Integer action : actions) {
            node = node.getNode(action);
        }
        node.setGame(game);
 
    }
    
    

    public boolean removeGame(Game game) {
        StateNode node = findNode(game.getGameDTO().getActionHistory(), game.getGameDTO().getActionHistory().size()-1);
        if (node == null) return false;
        while(node != null && node.children.isEmpty()) {
            StateNode parent = node.getParent();
            if (parent != null) {
                parent.children.remove(node);
                node.parent = null;
            }
            node = parent;
        }
        return true;
    }

    public StateNode findNode(List<Integer> actions, int pos) {
        StateNode node = root;
        for (int i = 0; i < pos; i++) {
            Integer action = actions.get(i);
            node = node.children.get(action);
            if (node == null) return null;
        }
        return node;
    }
}
