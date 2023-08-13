package ai.enpasos.muzero.platform.agent.c_planning;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.b_episode.Player;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.c_planning.GumbelFunctions.drawActions;
import static ai.enpasos.muzero.platform.agent.c_planning.GumbelFunctions.sigmas;
import static ai.enpasos.muzero.platform.agent.c_planning.SequentialHalving.initGumbelInfo;
import static ai.enpasos.muzero.platform.common.Functions.*;
import static ai.enpasos.muzero.platform.config.PlayTypeKey.HYBRID;
import static ai.enpasos.muzero.platform.config.PlayerMode.TWO_PLAYERS;

/**
 * Per game responsible for the rational search
 */
@Data
@Slf4j
public class GumbelSearch {
    Node root;

    SequentialHalving sequentialHalfingInfo;
    boolean simulationsFinished = false;

    Game game;
    Action selectedAction;
    MinMaxStats minMaxStatsQValues;

    MinMaxStats minMaxStatsEntropyQValues;
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
        this.minMaxStatsQValues = new MinMaxStats(config.getKnownBounds());

        this.minMaxStatsEntropyQValues = new MinMaxStats(config.getKnownBoundsEntropyQValues());

        int n = config.getNumSimulations(game);
        int m = config.getInitialGumbelM();
        int k = game.legalActions().size();
        this.sequentialHalfingInfo = initGumbelInfo(n, m, k);
        if (debug) log.trace(sequentialHalfingInfo.toString());

        currentPhase = 0;

        rootChildrenCandidates = new HashMap<>();
        IntStream.range(0, this.sequentialHalfingInfo.getPhaseNum()).forEach(i -> rootChildrenCandidates.put(i, new ArrayList<>()));
    }

    public static void storeSearchStatistics(Game game, TimeStepDO timeStepDO, @NotNull Node root, boolean justPriorValues, MuZeroConfig config, Action selectedAction, MinMaxStats minMaxStats, MinMaxStats minMaxStatsEntropyQValues) {
        timeStepDO.setRootValueTarget((float) root.getImprovedValue());
        timeStepDO.setVMix((float) root.getVmix()); timeStepDO.setVMix((float) root.getVmix());
        timeStepDO.setRootEntropyValueTarget((float)  root.getImprovedEntropyValue());

        float[] policyTarget = new float[config.getActionSpaceSize()];
        if (justPriorValues) {
            root.getChildren().forEach(node -> policyTarget[node.getAction().getIndex()] = (float) node.getPrior());
        } else if (root.getChildren().size() == 1) {
            policyTarget[selectedAction.getIndex()] = 1f;
        } else {

            double[] logits = root.getChildren().stream().mapToDouble(node -> node.getGumbelAction().getLogit()).toArray();

            double[] completedQsNormalized = root.getCompletedQValuesNormalized(minMaxStats);
            double[] completedEntropyQsNormalized = root.getCompletedQEntropyValuesNormalized(minMaxStatsEntropyQValues);

            int[] actions = root.getChildren().stream().mapToInt(node -> node.getAction().getIndex()).toArray();

            int maxActionVisitCount = root.getChildren().stream().mapToInt(Node::getVisitCount).max().getAsInt();
            double[] raw = add(logits, sigmas(
                    game.isItExplorationTime() ?  add(completedQsNormalized, completedEntropyQsNormalized) : completedQsNormalized
                    , maxActionVisitCount, config.getCVisit(), config.getCScale()));

            double[] improvedPolicy = softmax(raw);


            for (int i = 0; i < raw.length; i++) {
                int action = actions[i];
                policyTarget[action] = (float) improvedPolicy[i];
            }
        }
        timeStepDO.setPolicyTarget(policyTarget);
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
        gumbelActions = drawGumbelActionsInitially(gumbelActions, sequentialHalfingInfo.getM());

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
        gumbelActions = drawGumbelActions(1d, gumbelActions, sequentialHalfingInfo.getM(), config.getCVisit(), config.getCScale(), maxActionVisitCount);

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

//    public int drawGumbelActionFromAllRootChildren(double temperature) {
//        int cVisit = config.getCVisit();
//        double cScale = config.getCScale();
//        List<GumbelAction> gumbelActions = root.children.stream().map(Node::getGumbelAction).collect(Collectors.toList());
//        int maxActionVisitCount = root.children.stream().mapToInt(Node::getVisitCount).max().getAsInt();
//
//        int[] actions = gumbelActions.stream().mapToInt(GumbelAction::getActionIndex).toArray();
//        double[] raw = getLogitsAndQs(temperature,true, gumbelActions, cVisit, cScale, maxActionVisitCount);
//
//        IntStream.range(0, root.children.size()).forEach(i -> root.children.get(i).setPseudoLogit(raw[i]));
//
//        return drawActions(actions, raw, 1).get(0);
//    }


    private double[] getLogitsAndQs(double temperature, boolean withGumbel, List<GumbelAction> gumbelActions, int cVisit, double cScale, int maxActionVisitCount) {
        double[] g = gumbelActions.stream().mapToDouble(GumbelAction::getGumbelValue).toArray();
        double[] logits = gumbelActions.stream().mapToDouble(GumbelAction::getLogit).toArray();
        double[] qs = gumbelActions.stream()
            .mapToDouble(GumbelAction::getQValue)
            .map(v -> minMaxStatsQValues.normalize(v))
            .toArray();


        double scale = config.getEntropyContributionToReward();
        double[] scaledEntropyValue = gumbelActions.stream()
                .mapToDouble(GumbelAction::getEntropyQValue)
                .map(v -> minMaxStatsEntropyQValues.normalize(v))
                .map(v -> scale * v)
                .toArray();


        double[] sigmas = sigmas(add(qs, scaledEntropyValue), maxActionVisitCount, cVisit, cScale);
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
        return rootChildrenCandidates.get(currentPhase).get(this.sequentialHalfingInfo.im);
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
            log.trace(this.getSequentialHalfingInfo().toString());
        this.sequentialHalfingInfo.next();
        if (this.debug) {
            log.trace(this.getSequentialHalfingInfo().toString());
        }
        if (sequentialHalfingInfo.isFinished()) {
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
            if (this.sequentialHalfingInfo.isPhaseChanged()) {
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


    public void expand(NetworkIO networkOutput) {
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



    }


    public void backpropagate(NetworkIO networkOutput,  double discount) {
        double value = networkOutput.getValue();
        double entropyValue = networkOutput.getDidit();
        List<Node> searchPath = getCurrentSearchPath();
        Node node1 = searchPath.get(searchPath.size() - 1);
        Player toPlay = node1.getParent().getToPlay();
        if (debug) {
            log.trace("player at root: " + toPlay);
            log.trace("player at node: " + searchPath.get(searchPath.size() - 1).getToPlay());
        }

        boolean start = true;

        for (int i = searchPath.size() - 1; i >= 0; i--) {
            Node node = searchPath.get(i);
            node.setVisitCount(node.getVisitCount() + 1);

            if (start) {
                node.setValueFromInference(value);
                node.setEntropyValueFromInference(entropyValue);
                node.setImprovedValue(node.getValueFromInference());
                node.setImprovedEntropyValue(node.getEntropyValueFromInference());
                node.setImprovedPolicyValue(node.getPrior());
                start = false;
            } else {
                node.calculateVmix();
                node.calculateEntropyVmix();
                node.calculateImprovedPolicy(minMaxStatsQValues, minMaxStatsEntropyQValues, game.isItExplorationTime());
                node.calculateImprovedValue();
                node.calculateImprovedEntropyValue();
            }

            value =  node.getReward()
                    + (config.getPlayerMode() == PlayerMode.TWO_PLAYERS ? -1 : 1) * discount * value;

            entropyValue = node.getEntropyReward() + discount * entropyValue;

            node.setQValueSum(node.getQValueSum() + value);
            node.setEntropyQValueSum(node.getEntropyQValueSum() + entropyValue);

            minMaxStatsEntropyQValues.update(entropyValue);
            minMaxStatsQValues.update(value);
        }
    }

    public void storeSearchStatictics(  boolean fastRuleLearning, TimeStepDO timeStepDO) {
        storeSearchStatistics(game, timeStepDO, root, fastRuleLearning, config, selectedAction, minMaxStatsQValues, minMaxStatsEntropyQValues);
    }

    public Action selectAction( boolean fastRuleLearning, boolean replay, TimeStepDO timeStepDO, boolean hybrid2 ) {

        Action action = null;

        try {
            if (replay || (hybrid2 && timeStepDO.getT() < game.getEpisodeDO().getTStartNormal())) {
                return config.newAction(game.getOriginalEpisodeDO().getTimeSteps().get(timeStepDO.getT()).getAction());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (fastRuleLearning) {
            if (!replay) {
                timeStepDO.setPlayoutPolicy(timeStepDO.getPolicyTarget());
            }
            return root.getRandomAction();
        }

        if ( config.getTrainingTypeKey() != HYBRID && config.isGumbelActionSelection()) {
            return selectedAction;

        }


        double temperature = config.getTemperatureRoot();

        float[] policyTarget = timeStepDO.getPolicyTarget();
        double[] raw = new double[policyTarget.length];
        for (int i = 0; i < policyTarget.length; i++) {
            raw[i] = Math.log(policyTarget[i]);
        }
        if (config.getTrainingTypeKey() == HYBRID) {
            if (this.game.isItExplorationTime()) {
                    action = getAction(temperature, raw, game, timeStepDO, true);
            } else {
                //  the Gumbel selection
                if (config.isGumbelActionSelection()) {
                    timeStepDO.setPlayoutPolicy(toFloat(softmax(raw, 1d)));
                    action = selectedAction;
                } else {
                    action = getAction(1d, raw, game, timeStepDO, false);
                }
            }
        } else {
            action = getAction(temperature, raw, game, timeStepDO, false);
        }
        return action;

    }



    private Action getAction(double temperature, double[] raw, Game game,  TimeStepDO timeStepDO, boolean isExplorationTime) {
        Action action;
        // double[] p = softmax(raw, temperature);

        double[] p = null;
        if (isExplorationTime) {
            // try a different exploration scheme that
            // rescales the logits to a given interval


             p = softmax(rescaleLogitsIfOutsideInterval(raw, 6.0),temperature);
// the normal way
        //    p = softmax(raw, temperature);
        } else {
            p = softmax(raw, temperature);
        }

        timeStepDO.setPlayoutPolicy(toFloat(p));
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
        float v = (float) this.root.getValueFromInference();
        vs.add(v);
    }

    public void addExplorationNoise() {
        root.addExplorationNoise(config);
    }


}
