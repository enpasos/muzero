package ai.enpasos.muzero.platform.agent.rational;

import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.add;
import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.drawActions;
import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.sigmas;
import static ai.enpasos.muzero.platform.agent.rational.GumbelInfo.initGumbelInfo;
import static ai.enpasos.muzero.platform.agent.rational.SelfPlay.storeSearchStatistics;
import static ai.enpasos.muzero.platform.common.Functions.draw;
import static ai.enpasos.muzero.platform.common.Functions.softmax;
import static ai.enpasos.muzero.platform.common.Functions.toDouble;
import static ai.enpasos.muzero.platform.common.Functions.toFloat;
import static ai.enpasos.muzero.platform.config.PlayTypeKey.HYBRID;
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
    double pRandomActionRawAverage;
    int currentPhase;
    private Map<Integer, List<Node>> rootChildrenCandidates;

    public GumbelSearch(MuZeroConfig config, Game game, boolean debug, double pRandomActionRawAverage) {
        this.pRandomActionRawAverage = pRandomActionRawAverage;
        this.debug = debug;
        this.config = config;
        this.root = new Node(config, 0, true);
        this.game = game;
        this.minMaxStats = new MinMaxStats(config.getKnownBounds());

        int n = config.getNumSimulations(game);
        int m = config.getInitialGumbelM();
        int k = game.legalActions().size();
        this.gumbelInfo = initGumbelInfo(n, m, k);
        if (debug) log.trace(gumbelInfo.toString());

        currentPhase = 0;

        rootChildrenCandidates = new HashMap<>();
        IntStream.range(0, this.gumbelInfo.getPhaseNum()).forEach(i -> rootChildrenCandidates.put(i, new ArrayList<>()));
    }


    public void expandRootNode(boolean fastRuleLearning, NetworkIO networkOutput) {
        List<Action> legalActions = this.game.legalActions();
        if (legalActions.size() < 2) {
            simulationsFinished = true;
        }
        root.expandRootNode(this.game.toPlay(), legalActions, networkOutput, fastRuleLearning);

    }

    public void gumbelActionsStart(boolean withRandomness) {
        List<GumbelAction> gumbelActions = root.getChildren().stream().map(node -> {
            node.initGumbelAction(node.getAction().getIndex(), node.getPrior(), withRandomness);
            return node.getGumbelAction();
        }).collect(Collectors.toList());

        // drawing m actions out of the allowed actions (from root) for each parallel played game
        gumbelActions = drawGumbelActionsInitially(gumbelActions, gumbelInfo.getM());

        List<GumbelAction> gumbelActionsFinal = gumbelActions;
        rootChildrenCandidates.put(currentPhase,
            this.root.getChildren().stream()
                .filter(node -> gumbelActionsFinal.contains(node.getGumbelAction()))
                .collect(Collectors.toList()));

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

        List<GumbelAction> gumbelActions = rootChildrenCandidates.get(currentPhase).stream().map(Node::getGumbelAction).collect(Collectors.toList());
        int maxActionVisitCount = rootChildrenCandidates.get(currentPhase).stream().mapToInt(Node::getVisitCount).max().getAsInt();

        // drawing m actions out of the candidate actions (from root) for each parallel played game
        gumbelActions = drawGumbelActions(1d, gumbelActions, gumbelInfo.getM(), config.getCVisit(), config.getCScale(), maxActionVisitCount);

        List<GumbelAction> gumbelActionsFinal = gumbelActions;
        currentPhase++;
        rootChildrenCandidates.put(currentPhase, this.root.getChildren().stream()
            .filter(node -> gumbelActionsFinal.contains(node.getGumbelAction())).collect(Collectors.toList()));

    }

    public List<GumbelAction> drawGumbelActions(double temperature, List<GumbelAction> gumbelActions, int m, int cVisit, double cScale, int maxActionVisitCount) {
        int[] actions = gumbelActions.stream().mapToInt(GumbelAction::getActionIndex).toArray();
        double[] raw = getLogitsAndQs(temperature,true, gumbelActions, cVisit, cScale, maxActionVisitCount);

        IntStream.range(0, rootChildrenCandidates.get(currentPhase).size()).forEach(i -> rootChildrenCandidates.get(currentPhase).get(i).setPseudoLogit(raw[i]));

        List<Integer> selectedActions = drawActions(actions, raw, m);
        return gumbelActions.stream().filter(a -> selectedActions.contains(a.actionIndex)).collect(Collectors.toList());
    }

    public int drawGumbelActionFromAllRootChildren(double temperature) {
        int cVisit = config.getCVisit();
        double cScale = config.getCScale();
        List<GumbelAction> gumbelActions = root.children.stream().map(Node::getGumbelAction).collect(Collectors.toList());
        int maxActionVisitCount = root.children.stream().mapToInt(Node::getVisitCount).max().getAsInt();

        int[] actions = gumbelActions.stream().mapToInt(GumbelAction::getActionIndex).toArray();
        double[] raw = getLogitsAndQs(temperature,true, gumbelActions, cVisit, cScale, maxActionVisitCount);

        IntStream.range(0, root.children.size()).forEach(i -> root.children.get(i).setPseudoLogit(raw[i]));

        return drawActions(actions, raw, 1).get(0);
    }


    private double[] getLogitsAndQs(double temperature, boolean withGumbel, List<GumbelAction> gumbelActions, int cVisit, double cScale, int maxActionVisitCount) {
        double[] g = gumbelActions.stream().mapToDouble(GumbelAction::getGumbelValue).toArray();
        double[] logits = gumbelActions.stream().mapToDouble(GumbelAction::getLogit).toArray();
        double[] qs = gumbelActions.stream()
            .mapToDouble(GumbelAction::getQValue)
            .map(v -> minMaxStats.normalize(v))
            .toArray();

        double[] sigmas = sigmas(qs, maxActionVisitCount, cVisit, cScale);
        double[] logitsPlusSigmas = new double[logits.length];
            IntStream.range(0, logits.length).forEach(i -> {
                logitsPlusSigmas[i] = logits[i] + sigmas[i];
                if (temperature != 1d) {
                    logitsPlusSigmas[i] /= temperature;
                }
            } );
        if (withGumbel) {
            return add(logitsPlusSigmas, g);
        } else {
            return logitsPlusSigmas;
        }
    }

    public Node getCurrentRootChild() {
        return rootChildrenCandidates.get(currentPhase).get(this.gumbelInfo.im);
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
            node = node.selectChild();
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
            if (this.getRootChildrenCandidates().get(currentPhase).size() > 1) {
                // find the phaseNum with the most visited actions
                int maxPhaseNum = getPhaseNumWithTheMostVisitedActions();

                List<GumbelAction> gumbelActions = this.rootChildrenCandidates.get(maxPhaseNum).stream().map(Node::getGumbelAction).collect(Collectors.toList());
                int maxActionVisitCount = this.rootChildrenCandidates.get(maxPhaseNum).stream().mapToInt(Node::getVisitCount).max().getAsInt();

                this.selectedAction = drawGumbelActions(1d, gumbelActions, 1, config.getCVisit(), config.getCScale(), maxActionVisitCount).get(0).node.getAction();

            } else {
                this.selectedAction = this.getRootChildrenCandidates().get(currentPhase).get(0).getAction();
                simulationsFinished = true;
            }
            if (debug) log.debug("simulation finished");
        } else {
            if (this.gumbelInfo.isPhaseChanged()) {
                gumbelActionsOnPhaseChange();
            }
        }

    }

    private int getPhaseNumWithTheMostVisitedActions() {
        int maxVisitCounts = 0;
        int phaseNum = 0;
        for (Map.Entry<Integer, List<Node>> e : this.getRootChildrenCandidates().entrySet()) {
            int vc = e.getValue().stream().mapToInt(Node::getVisitCount).sum();
            if (vc > maxVisitCounts) {
                maxVisitCounts = vc;
                phaseNum = e.getKey();
            }
        }
        return phaseNum;
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

//        if (game.isRecordValueImprovements()) {
//            double vImprovement = root.getImprovedValue() - root.getValueFromInitialInference();
//            game.getValueImprovements().add(vImprovement);
//        }

    }


    public void backUp(double value, Player toPlay, double discount) {
        List<Node> searchPath = getCurrentSearchPath();

        if (debug) {
            log.trace("player at root: " + toPlay);
            log.trace("player at node: " + searchPath.get(searchPath.size() - 1).getToPlay());
        }


        boolean start = true;

        for (int i = searchPath.size() - 1; i >= 0; i--) {
            Node node = searchPath.get(i);
            node.setVisitCount(node.getVisitCount() + 1);

            if (start) {
                node.setValueFromNetwork(value);
                node.setImprovedValue(node.getValueFromNetwork());
                node.setImprovedPolicyValue(node.getPrior());
                start = false;
            } else {
                node.calculateVmix();
                node.calculateImprovedPolicy(minMaxStats);
                node.calculateImprovedValue();

            }

            value = node.getReward() + (config.getPlayerMode() == PlayerMode.TWO_PLAYERS ? -1 : 1) * discount * value;

            node.setQValueSum(node.getQValueSum() + value);
            minMaxStats.update(value);
        }
    }

    public void storeSearchStatictics(boolean render, boolean fastRuleLearning) {
        storeSearchStatistics(game, root, fastRuleLearning, config, selectedAction, minMaxStats);
    }

    public void selectAndApplyAction(boolean render, boolean fastRuleLearning, boolean replay ) {

        Action action = null;

        if (replay) {
            return;
       }

        if (fastRuleLearning) {
            action = root.getRandomAction();
            applyAction(render, action);
            this.game.getGameDTO().getPlayoutPolicy().add(this.game.getGameDTO().getPolicyTargets().get(this.game.getGameDTO().getPolicyTargets().size() - 1));
            return;
        }

        if ( config.getTrainingTypeKey() != HYBRID && config.isGumbelActionSelection()) {
            action = selectedAction;
            applyAction(render, action);
            this.game.getGameDTO().getPlayoutPolicy().add(this.game.getGameDTO().getPolicyTargets().get(this.game.getGameDTO().getPolicyTargets().size() - 1));
            return;
        }


        double temperature = config.getTemperatureRoot();

        float[] policyTarget = game.getGameDTO().getPolicyTargets().get(game.getGameDTO().getPolicyTargets().size() - 1);
        double[] raw = new double[policyTarget.length];
        for (int i = 0; i < policyTarget.length; i++) {
            raw[i] = Math.log(policyTarget[i]);
        }
        if (config.getTrainingTypeKey() == HYBRID) {
            if (this.game.getGameDTO().getActions().size() < this.game.getGameDTO().getTHybrid()) {
                if (config.isGumbelActionSelectionOnExploring()) {
                    game.getGameDTO().getPlayoutPolicy().add(toFloat(softmax(raw, temperature)));
                    action = config.newAction(drawGumbelActionFromAllRootChildren(temperature));
                } else {
                    action = getAction(temperature, raw, game);
                }
            } else {
                //  the Gumbel selection
                if (config.isGumbelActionSelection()) {
                    game.getGameDTO().getPlayoutPolicy().add(toFloat(softmax(raw, 1d)));
                    action = selectedAction;
                } else {
                    action = getAction(1d, raw, game);
                }
            }
        } else {
            action = getAction(temperature, raw, game);
        }
        applyAction(render, action);

    }

    private void applyAction(boolean render, Action action) {
        if (action == null) {
            throw new MuZeroException("action must not be null");
        }

        game.apply(action);

        if (render && debug) {
            List<float[]> policyTargets = game.getGameDTO().getPolicyTargets();
            float[] policyTarget = policyTargets.get(policyTargets.size() - 1);
            game.renderMCTSSuggestion(config, policyTarget);
            log.info("\n" + game.render());
        }
    }

    private Action getAction(double temperature, double[] raw, Game game) {
        Action action;
        double[] p = softmax(raw, temperature);
        game.getGameDTO().getPlayoutPolicy().add(toFloat(p));
        int i = draw(p);
        action = config.newAction(i);
        return action;
    }



    public void drawCandidateAndAddValue() {
        List<GumbelAction> gumbelActions = rootChildrenCandidates.get(currentPhase).stream().map(Node::getGumbelAction).collect(Collectors.toList());
        int maxActionVisitCount = rootChildrenCandidates.get(currentPhase).stream().mapToInt(Node::getVisitCount).max().getAsInt();
        drawGumbelActions(1d, gumbelActions, 1, config.getCVisit(), config.getCScale(), maxActionVisitCount).get(0);
    }

    public void drawCandidateAndAddValueStart() {
        List<Float> vs = new ArrayList<>();
        float v = (float) this.root.getValueFromNetwork();
        vs.add(v);
    }

    public void addExplorationNoise() {
        root.addExplorationNoise(config);
    }


}
