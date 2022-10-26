package ai.enpasos.muzero.platform.agent.memorize.tree;

import ai.enpasos.muzero.platform.agent.memorize.Game;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
@Slf4j
public class NodeDTO {
    private Integer action;
    private Map<Integer, Double> valuePerEpoch;
    private Map<Integer, Integer> visitsPerEpoch;

    private Map<Integer, NodeDTO> childPerAction;

    public NodeDTO() {
        valuePerEpoch = new TreeMap<>();
        visitsPerEpoch = new TreeMap<>();
        childPerAction = new TreeMap<>();
    }

    public boolean isRoot() {
        return action == null;
    }

    public void memorize(List<Game> games, int epoch) {
        games.stream().forEach(game ->
            memorize(game, epoch)
        );
    }

    public void memorize(Game game, int epoch) {
        memorize(game, epoch, 0);
    }

    private void memorize(Game game, int epoch, int index) {
        List<Integer> actions = game.getGameDTO().getActions();
        if (index > actions.size() - 1 || index > game.getGameDTO().getRootValuesFromInitialInference().size() - 1) {
            return;
        }
        int actionLocal = actions.get(index);

        childPerAction.computeIfAbsent(actionLocal, k -> new NodeDTO());

        double value = game.getGameDTO().getRootValuesFromInitialInference().get(index);
        this.valuePerEpoch.put(epoch, value);
        int count = 1;
        if (this.visitsPerEpoch.containsKey(epoch)) {
            count += this.visitsPerEpoch.get(epoch);
        }
        this.visitsPerEpoch.put(epoch, count);

        NodeDTO child = childPerAction.get(actionLocal);
        child.memorize(game, epoch, index + 1);
    }
}
