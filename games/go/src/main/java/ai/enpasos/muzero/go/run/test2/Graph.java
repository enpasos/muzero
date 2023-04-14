package ai.enpasos.muzero.go.run.test2;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.episode.Player;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class Graph {
    GNode root;

    @Autowired
    MuZeroConfig config;


    int perspective;


    Map<StateKey, GNode> allNodes = new HashMap<>();

    public void init(StateKey rootStateKey) {
        root = new GNode(rootStateKey);
        allNodes.put(rootStateKey, root);
    }



    public GNode getRoot() {
        return root;
    }

    public void applyAction(GNode fromNode, int action, StateKey newStateKey) {

        allNodes.putIfAbsent(newStateKey, new GNode(newStateKey));
        GNode toNode = allNodes.get(newStateKey);

        GRelation relation = GRelation.builder()
                .action(action)
                .from(fromNode)
                .to(toNode)
                .build();
         relation.attachToNodes();
    }


    public GNode getNode(StateKey stateKey) {
        return allNodes.get(stateKey);
    }

}
