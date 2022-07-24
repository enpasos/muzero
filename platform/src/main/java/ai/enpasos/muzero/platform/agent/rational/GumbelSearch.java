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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.*;
import static ai.enpasos.muzero.platform.agent.rational.GumbelInfo.initGumbelInfo;
import static ai.enpasos.muzero.platform.agent.rational.SelfPlay.storeSearchStatistics;
import static ai.enpasos.muzero.platform.common.Functions.*;
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


    double pRandomActionRawAverage; //TODO

    public GumbelSearch(MuZeroConfig config, Game game, boolean debug, double pRandomActionRawAverage) {
        this.pRandomActionRawAverage = pRandomActionRawAverage;
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

            node.setValueSum(node.getValueSum() + value);

            minMaxStats.update(value);


        }
    }

    public void selectAndApplyActionAndStoreSearchStatistics(boolean render, boolean fastRuleLearning, boolean withRandomActions) {

        Action action;

        storeSearchStatistics(game, root, fastRuleLearning, config, selectedAction, minMaxStats);

        if (fastRuleLearning) {
            action = root.getRandomAction();
        } else {
            if (config.getTemperatureRoot() == 0.0) {
                action = selectedAction;
           } else {
                float[] policyTarget = game.getGameDTO().getPolicyTargets().get(game.getGameDTO().getPolicyTargets().size()-1);
                double[] raw = new double[policyTarget.length];
                for (int i = 0; i < policyTarget.length; i++) {
                    raw[i] = Math.log(policyTarget[i]);
                }
                double[] p = softmax(raw, config.getTemperatureRoot());
                int a = draw(p);
                action = config.newAction(a);
            }
         }
        if (action == null) {
            throw new MuZeroException("action must not be null");
        }

        if (withRandomActions) {
            Optional<Action> optionalAction = badAction(render );
            if (optionalAction.isPresent()) {
                action = optionalAction.get();
            }
        }
        game.apply(action);

        if (render && debug) {
            List<float[]> policyTargets = game.getGameDTO().getPolicyTargets();
            float[] policyTarget = policyTargets.get(policyTargets.size() - 1);
            game.renderMCTSSuggestion(config, policyTarget);
            log.debug("\n" + game.render());
        }


    }

    private Optional<Action> badAction(boolean render) {

        int[] legalActions = game.legalActions().stream().mapToInt(Action::getIndex).toArray();

        double[]  pRational_ = toDouble(this.game.getGameDTO().getPolicyTargets().get(this.game.getGameDTO().getPolicyTargets().size() - 1));

        double[]  pRational = IntStream.range(0,legalActions.length).mapToDouble(i -> pRational_[legalActions[i]]).toArray();


        double[]  pIntuitive = this.root.children.stream().mapToDouble(Node::getPrior).toArray();

        double[]  p_perLegalActionRaw  =  new double[legalActions.length];

        IntStream.range(0,legalActions.length).forEach( i ->
            p_perLegalActionRaw[i] = (1.0 - pIntuitive[i])  * (pRational[i] > pIntuitive[i] ? pRational[i] - pIntuitive[i]: 0.0) // / (1.0 + this.game.getGameDTO().getActions().size())
        );

        this.game.getGameDTO().setPRandomActionRawSum(
            this.game.getGameDTO().getPRandomActionRawSum() + (float)Arrays.stream(p_perLegalActionRaw).sum()
        );
        this.game.getGameDTO().setPRandomActionRawCount(
            this.game.getGameDTO().getPRandomActionRawCount() + p_perLegalActionRaw.length
        );

        if (pRandomActionRawAverage == 0  || game.getGameDTO().getTStateA() != 0) return Optional.empty();

        float fraction = config.getAlternativeActionsWeight();
        double[]  p  =  Arrays.stream(p_perLegalActionRaw).map(pRaw ->
            config.getAlternativeActionsWeight() * pRaw /  pRandomActionRawAverage
        ).toArray();


        double  p_perGame = Arrays.stream(p).sum();

        if (!draw( p_perGame)) return Optional.empty();

        p =  Arrays.stream(p).map(b ->
            b / p_perGame
        ).toArray();

        int numLegalActions = game.legalActions().size();
        Action action = config.newAction(legalActions[draw(p)]);


        game.getGameDTO().setTStateA(game.getGameDTO().getActions().size());
        game.getGameDTO().setTStateB(game.getGameDTO().getActions().size());
        if (render && game.isDebug()) {
            game.renderMCTSSuggestion(config, game.getGameDTO().getPolicyTargets().get(game.getGameDTO().getPolicyTargets().size()-1));
            log.debug("\n" + game.render());
        }
        game.setActionApplied(true);
return Optional.of(action);
    }

    private double[] getProbabilitiesPerLegalAction() {
        List<Action> legalActions = game.legalActions();

        float[] policyTarget = this.game.getGameDTO().getPolicyTargets().get(this.game.getGameDTO().getPolicyTargets().size()-1);


        double[] p_perLegalActionRaw = new double[game.legalActions().size()];
        for (int i = 0; i < p_perLegalActionRaw.length; i++) {
            p_perLegalActionRaw[i] = 1.0 / p_perLegalActionRaw.length;
        }
        return p_perLegalActionRaw;
    }

    public void drawCandidateAndAddValue() {
        List<GumbelAction> gumbelActions = rootChildrenCandidates.stream().map(Node::getGumbelAction).collect(Collectors.toList());


        int maxActionVisitCount = rootChildrenCandidates.stream().mapToInt(Node::getVisitCount).max().getAsInt();

        // drawing 1 action out of the candidate actions (from root) for each parallel played game

        GumbelAction gumbelAction = drawGumbelActions(gumbelActions, 1, config.getCVisit(), config.getCScale(), maxActionVisitCount).get(0);
        List<Float> values = this.game.getGameDTO().getValues().get(this.game.getGameDTO().getValues().size() - 1);
        values.add((float) gumbelAction.getNode().getQValue());
    }

    public void drawCandidateAndAddValueStart() {
        List<Float> vs = new ArrayList<>();
        float v = (float) this.root.getValueFromNetwork();
        vs.add(v);
        this.game.getGameDTO().getValues().add(vs);
    }

    public void addExplorationNoise() {
        root.addExplorationNoise(config);
    }


}
