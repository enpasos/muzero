package ai.enpasos.muzero.platform.agent.rational.gumbel;

import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.agent.rational.MinMaxStats;
import ai.enpasos.muzero.platform.agent.rational.Node;
import ai.enpasos.muzero.platform.agent.rational.Player;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.rational.EpisodeManager.getRandomAction;
import static ai.enpasos.muzero.platform.agent.rational.EpisodeManager.softmax;
import static ai.enpasos.muzero.platform.agent.rational.gumbel.Gumbel.add;
import static ai.enpasos.muzero.platform.agent.rational.gumbel.Gumbel.drawActions;
import static ai.enpasos.muzero.platform.agent.rational.gumbel.GumbelInfo.initGumbelInfo;
import static ai.enpasos.muzero.platform.agent.rational.gumbel.SequentialHalving.extraPhaseVisitsToUpdateQPerPhase;
import static ai.enpasos.muzero.platform.agent.rational.gumbel.SequentialHalving.sigmas;
import static ai.enpasos.muzero.platform.config.PlayerMode.TWO_PLAYERS;

/**
 * Per game responsible for the rational search
 */
@Data
@Slf4j
public class SearchManager {
    Node root;

    GumbelInfo gumbelInfo;
    boolean simulationsFinished = false;

    Game game;
    Action selectedAction;
    MinMaxStats minMaxStats;
    MuZeroConfig config;
    boolean debug;
    private List<Node> rootChildrenCandidates;

    public SearchManager(MuZeroConfig config, Game game, boolean debug) {
        this.debug = debug;
        this.config = config;
        this.root = new Node(config, 0, true);
        this.game = game;
        this.minMaxStats = new MinMaxStats(config.getKnownBounds());

        int n = config.getNumSimulations();
        int m = Math.min(n, config.getInitialGumbelM());
        int k = game.legalActions().size();
        this.gumbelInfo = initGumbelInfo(n, m, k);
        if (debug) log.trace(gumbelInfo.toString());
    }


    public void expandRootNode(boolean fastRuleLearning, NetworkIO networkOutput) {
        List<Action> legalActions = this.game.legalActions();
        if (legalActions.size() < 2) {
            simulationsFinished = true;
        }
        root.expandRootNode(this.game.toPlay(), legalActions, networkOutput, fastRuleLearning);
    }

    public void gumbelActionsStart() {
        List<GumbelAction> gumbelActions = root.getChildren().stream().map(node -> {
            node.initGumbelAction(node.getAction().getIndex(), node.getPrior());
            return node.getGumbelAction();
        }).collect(Collectors.toList());

        // drawing m actions out of the allowed actions (from root) for each parallel played game
        gumbelActions = drawGumbelActionsInitially(gumbelActions, gumbelInfo.getM());

        List<GumbelAction> gumbelActionsFinal = gumbelActions;
        rootChildrenCandidates = this.root.getChildren().stream()
            .filter(node -> gumbelActionsFinal.contains(node.getGumbelAction())).collect(Collectors.toList());

    }

    public List<GumbelAction> drawGumbelActionsInitially(List<GumbelAction> gumbelActions, int n) {
        int[] actions = gumbelActions.stream().mapToInt(GumbelAction::getActionIndex).toArray();
        double[] g = gumbelActions.stream().mapToDouble(GumbelAction::getGumbelValue).toArray();
        double[] logits = gumbelActions.stream().mapToDouble(GumbelAction::getLogit).toArray();
        double[] raw = add(logits, g);

        List<Integer> selectedActions = drawActions(actions, raw, n);
        return gumbelActions.stream().filter(a -> selectedActions.contains(a.actionIndex)).collect(Collectors.toList());
    }

    public void gumbelActionsOnPhaseChange() {

        List<GumbelAction> gumbelActions = rootChildrenCandidates.stream().map(Node::getGumbelAction).collect(Collectors.toList());


        int maxActionVisitCount = rootChildrenCandidates.stream().mapToInt(Node::getVisitCount).max().getAsInt();

        // drawing m actions out of the candidate actions (from root) for each parallel played game
        gumbelActions = drawGumbelActions(gumbelActions, gumbelInfo.getM(), config.getCVisit(), config.getCScale(), maxActionVisitCount);

        List<GumbelAction> gumbelActionsFinal = gumbelActions;
        rootChildrenCandidates = this.root.getChildren().stream()
            .filter(node -> gumbelActionsFinal.contains(node.getGumbelAction())).collect(Collectors.toList());

    }

    public List<GumbelAction> drawGumbelActions(List<GumbelAction> gumbelActions, int m, int cVisit, double cScale, int maxActionVisitCount) {
        int[] actions = gumbelActions.stream().mapToInt(GumbelAction::getActionIndex).toArray();
        double[] g = gumbelActions.stream().mapToDouble(GumbelAction::getGumbelValue).toArray();
        double[] logits = gumbelActions.stream().mapToDouble(GumbelAction::getLogit).toArray();
        double[] qs = gumbelActions.stream()
            .mapToDouble(GumbelAction::getQValue)
            .map(v -> minMaxStats.normalize(v))
            .toArray();

        double[] sigmas = sigmas(qs, maxActionVisitCount, cVisit, cScale);

        double[] raw = add(add(logits, g), sigmas);

        IntStream.range(0, rootChildrenCandidates.size()).forEach(i -> rootChildrenCandidates.get(i).setPseudoLogit(raw[i]));

        List<Integer> selectedActions = drawActions(actions, raw, m);
        return gumbelActions.stream().filter(a -> selectedActions.contains(a.actionIndex)).collect(Collectors.toList());
    }

    public Node getCurrentRootChild() {
        return rootChildrenCandidates.get(this.gumbelInfo.im);
    }

    public List<Node> getCurrentSearchPath() {
        Node rootChild = getCurrentRootChild();
        return rootChild.getSearchPath();
    }

    public List<Node> search() {
        Node rootChild = getCurrentRootChild();
        rootChild.setSearchPath(new ArrayList<>());
        rootChild.getSearchPath().add(root);
        rootChild.getSearchPath().add(rootChild);
        Node node = rootChild;
        while (node.expanded()) {
            node = node.selectChild(minMaxStats);
            rootChild.getSearchPath().add(node);
        }
        return rootChild.getSearchPath();
    }

    public void next() {
        if (this.debug)
            log.trace(this.getGumbelInfo().toString());
        this.gumbelInfo.next();
        if (this.debug) {
            log.trace(this.getGumbelInfo().toString());
        }
        if (gumbelInfo.isFinished()) {
            gumbelActionsOnPhaseChange();
            if (this.getRootChildrenCandidates().size() > 1) {
                throw new MuZeroException("RootChildrenCandidates().size() > 1");
            }
            this.selectedAction = this.getRootChildrenCandidates().get(0).getAction();
            simulationsFinished = true;
            if (debug) log.debug("simulation finished");
        } else {
            if (this.gumbelInfo.isPhaseChanged()) {
                gumbelActionsOnPhaseChange();
            }
        }

    }


    public void expandAndBackpropagate(NetworkIO networkOutput) {
        List<Node> searchPath = getCurrentSearchPath();
        Node node = searchPath.get(searchPath.size() - 1);
        Player toPlayOnNode = node.getParent().getToPlay();
        if (config.getPlayerMode() == TWO_PLAYERS) {
            toPlayOnNode = OneOfTwoPlayer.otherPlayer((OneOfTwoPlayer) toPlayOnNode);
        }

        node.expand(toPlayOnNode, networkOutput);
        // networkOutput.getValue() is from the perspective of the player to play at the node
        if (debug) {
            log.trace("value from network at backUp: " + networkOutput.getValue());
        }

        backUp(networkOutput.getValue(), node.getToPlay(), this.config.getDiscount());
    }


    public void backUp(double value, Player toPlay, double discount) {
        List<Node> searchPath = getCurrentSearchPath();

        if (debug) {
            log.trace("player at root: " + toPlay);
            log.trace("player at node: " + searchPath.get(searchPath.size() - 1).getToPlay());
        }

        for (int i = searchPath.size() - 1; i >= 0; i--) {
            Node node = searchPath.get(i);
            double nodeValueSumBefore = node.getValueSum();
            int nodeVisitCountBefore = node.getVisitCount();
            if (node.getToPlay() == toPlay) {
                node.setValueSum(nodeValueSumBefore + value);
            } else {
                node.setValueSum(nodeValueSumBefore - value);
            }
            node.setVisitCount(nodeVisitCountBefore + 1);
            if (debug)
                log.trace("searchPath[" + i + "]: " + nodeValueSumBefore + "/" + nodeVisitCountBefore + "->" + node.getValueSum() + "/" + node.getVisitCount());

            value = node.getReward() + discount * value;
            minMaxStats.update(value);
        }
    }

    public void selectAndApplyActionAndStoreSearchStatistics(boolean render, boolean fastRuleLearning) {

        Action action;

        if (fastRuleLearning) {
            action = getRandomAction(root);
        } else {
            action = selectedAction;
        }
        if (action == null) {
            throw new MuZeroException("action must not be null");
        }
        game.apply(action);
        storeSearchStatistics(game, root, fastRuleLearning);


        if (render && debug) {
            List<float[]> policyTargets = game.getGameDTO().getPolicyTargets();
            float[] policyTarget = policyTargets.get(policyTargets.size() - 1);
            game.renderMCTSSuggestion(config, policyTarget);
            log.debug("\n" + game.render());
        }


    }

    public void storeSearchStatistics(Game game, @NotNull Node root, boolean fastRuleLearning) {

        float[] policyTarget = new float[config.getActionSpaceSize()];
        if (fastRuleLearning) {
            root.getChildren().forEach(node -> policyTarget[node.getAction().getIndex()] = (float) node.getPrior());
        } else if (root.getChildren().size() == 1) {
            policyTarget[this.getSelectedAction().getIndex()] = 1f;
        } else {

            double[] logits = root.getChildren().stream().mapToDouble(node -> node.getGumbelAction().getLogit()).toArray();

            double[] completedQs = root.getCompletedQValues(minMaxStats);

            int[] actions = root.getChildren().stream().mapToInt(node -> node.getAction().getIndex()).toArray();

            int maxActionVisitCount = root.getChildren().stream().mapToInt(Node::getVisitCount).max().getAsInt();
            double[] raw = add(logits, sigmas(completedQs, maxActionVisitCount, config.getCVisit(), config.getCScale()));

            double[] improvedPolicy = softmax(raw);

            for (int i = 0; i < raw.length; i++) {
                int action = actions[i];
                double v = improvedPolicy[i];
                root.getChildren().get(i).setImprovedPolicyValue(v);  // for debugging
                policyTarget[action] = (float) v;
            }
        }
        game.getGameDTO().getPolicyTargets().add(policyTarget);
        game.getGameDTO().getRootValuesFromInitialInference().add((float) root.getValueFromInitialInference());
    }

}
