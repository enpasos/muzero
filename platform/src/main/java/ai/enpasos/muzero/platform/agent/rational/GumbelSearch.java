package ai.enpasos.muzero.platform.agent.rational;

import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.*;
import static ai.enpasos.muzero.platform.agent.rational.GumbelInfo.initGumbelInfo;
import static ai.enpasos.muzero.platform.agent.rational.SelfPlay.storeSearchStatistics;
import static ai.enpasos.muzero.platform.common.Functions.softmax;
import static ai.enpasos.muzero.platform.config.PlayerMode.TWO_PLAYERS;

/**
 * Per game responsible for the rational search
 */
@Data
@Slf4j
public class GumbelSearch {
    Node root;

    GumbelInfo gumbelInfo;
    boolean simulationsFinished = false;

    Game game;
    Action selectedAction;
    MinMaxStats minMaxStats;
    MuZeroConfig config;
    boolean debug;
    private List<Node> rootChildrenCandidates;

    public GumbelSearch(MuZeroConfig config, Game game, boolean debug) {
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

        List<Integer> selectedActions = drawActions(actions, raw, n, config.getGumbelSoftmaxTemperature());
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

        List<Integer> selectedActions = drawActions(actions, raw, m, config.getGumbelSoftmaxTemperature());
        return gumbelActions.stream().filter(a -> selectedActions.contains(a.actionIndex)).collect(Collectors.toList());
    }

    public Node getCurrentRootChild() {
        return rootChildrenCandidates.get(this.gumbelInfo.im);
    }

    public List<Node> getCurrentSearchPath() {
        Node rootChild = getCurrentRootChild();
        return rootChild.getSearchPath();
    }

    public List<Node> search( ) {
        Node rootChild = getCurrentRootChild();
        rootChild.setSearchPath(new ArrayList<>());
        rootChild.getSearchPath().add(root);
        rootChild.getSearchPath().add(rootChild);
        Node node = rootChild;
        while (node.expanded()) {
            node = node.selectChild(minMaxStats );
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
       //     double nodeValueSumBefore = node.getValueSum();
            node.setVisitCount(node.getVisitCount() + 1);
            node.calculateVmix();
//            if (node.getToPlay() == toPlay) {
//                node.setValueSum(nodeValueSumBefore + value);
//            } else {
//                node.setValueSum(nodeValueSumBefore - value);
//            }


            double qValue = node.getReward() + (config.getPlayerMode() == PlayerMode.TWO_PLAYERS? -1: 1) * discount * node.getVmix();


            node.setQValue(qValue);

            minMaxStats.update(qValue);

        }
    }

    public void selectAndApplyActionAndStoreSearchStatistics(boolean render, boolean fastRuleLearning) {

        Action action;

        if (fastRuleLearning) {
            action = root.getRandomAction();
        } else {
            action = selectedAction;
        }
        if (action == null) {
            throw new MuZeroException("action must not be null");
        }
        game.apply(action);
        storeSearchStatistics(game, root, fastRuleLearning, config, selectedAction, minMaxStats);


        if (render && debug) {
            List<float[]> policyTargets = game.getGameDTO().getPolicyTargets();
            float[] policyTarget = policyTargets.get(policyTargets.size() - 1);
            game.renderMCTSSuggestion(config, policyTarget);
            log.debug("\n" + game.render());
        }


    }


}
