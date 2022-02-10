package ai.enpasos.muzero.platform.agent.rational;

import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.memory.Game;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import umontreal.ssj.randvarmulti.DirichletGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@Slf4j
@Component
public class Episode {
    private static final RandomStream randomStreamBase = new MRG32k3a("rnd");
    private long start;
    private Duration inferenceDuration;
    private List<Game> gameList;
    private List<Game> gamesDoneList;

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private RegularizedPolicyOptimization regularizedPolicyOptimization;


    @Autowired
    private MCTS mcts;

    private static Action getRandomAction(Node root) {
        SortedMap<Action, Node> children = root.getChildren();
        int index = randomStreamBase.nextInt(0, children.size() - 1);
        return ((Map.Entry<Action, Node>) children.entrySet().toArray()[index]).getKey();
    }

    public static void addExplorationNoise(double rootExplorationFraction, double rootDirichletAlpha, @NotNull Node node) {
        Action[] actions = node.getChildren().keySet().toArray(new Action[0]);
        double[] noise = numpyRandomDirichlet(rootDirichletAlpha, actions.length);
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            Node child = node.getChildren().get(action);
            child.setPrior((1 - rootExplorationFraction) * child.getPrior() + rootExplorationFraction * noise[i]);
        }
    }

    private static double @NotNull [] numpyRandomDirichlet(double alpha, int dims) {

        double[] alphas = new double[dims];
        Arrays.fill(alphas, alpha);
        DirichletGen dg = new DirichletGen(randomStreamBase, alphas);
        double[] p = new double[dims];

        dg.nextPoint(p);
        return p;
    }

    @PostConstruct
    public void postConstruct() {
        init();
    }

    public void init() {
        start = System.currentTimeMillis();
        inferenceDuration = new Duration();
        gameList = IntStream.rangeClosed(1, config.getNumParallelGamesPlayed())
                .mapToObj(i -> config.newGame())
                .collect(Collectors.toList());
        gamesDoneList = new ArrayList<>();
    }

    public void init(List<Game> inputGames) {
        start = System.currentTimeMillis();
        inferenceDuration = new Duration();
        gameList = new ArrayList<>();
        gameList.addAll(inputGames);
        gamesDoneList = new ArrayList<>();
    }


    public void justReplayWithInitialInference(Network network) {
        int indexOfJustOneOfTheGames = getGameList().indexOf(justOneOfTheGames());

        List<Node> rootList = initRootNodes();

        List<NetworkIO> networkOutput = initialInference(network, false, false, indexOfJustOneOfTheGames);

        IntStream.range(0, gameList.size()).forEach(g ->
        {
            Game game = gameList.get(g);
            Node root = rootList.get(g);

            root.setValueFromInitialInference(networkOutput.get(g).getValue());

            int nActionsReplayed = game.actionHistory().getActionIndexList().size();
            int actionIndex = game.getOriginalGameDTO().getActions().get(nActionsReplayed);

            try {
                game.apply(actionIndex);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new MuZeroException(e);
            }
            game.getGameDTO().getRootValuesFromInitialInference().add((float) root.getValueFromInitialInference());

        });


        keepTrackOfOpenGames();
    }

    public void play(Network network, boolean render, boolean fastRuleLearning, boolean explorationNoise) {
        int indexOfJustOneOfTheGames = getGameList().indexOf(justOneOfTheGames());

        List<Node> rootList = initRootNodes();

        List<NetworkIO> networkOutput = initialInference(network, render, fastRuleLearning, indexOfJustOneOfTheGames);

        expandRootNodes(fastRuleLearning, rootList, networkOutput);

        addExplorationNoise(render, explorationNoise, indexOfJustOneOfTheGames, rootList);

        final List<MinMaxStats> minMaxStatsList = runMCTS(network, fastRuleLearning, rootList, mcts);

        selectAndApplyActionAndStoreSearchStatistics(render, fastRuleLearning, indexOfJustOneOfTheGames, rootList, mcts, minMaxStatsList);

        renderNetworkGuess(network, render, indexOfJustOneOfTheGames);

        keepTrackOfOpenGames();
    }

    private void renderNetworkGuess(Network network, boolean render, int indexOfJustOneOfTheGames) {
        if (render && indexOfJustOneOfTheGames != -1 && justOneOfTheGames().terminal()) {
            NetworkIO networkOutput2 = network.initialInferenceDirect(gameList.get(indexOfJustOneOfTheGames));
            justOneOfTheGames().renderNetworkGuess(config, justOneOfTheGames().toPlay(), networkOutput2, true);
        }
    }

    private void keepTrackOfOpenGames() {
        List<Game> newGameDoneList = gameList.stream()
                .filter(game -> game.terminal() || game.getGameDTO().getActions().size() >= config.getMaxMoves())
                .collect(Collectors.toList());

        gamesDoneList.addAll(newGameDoneList);
        gameList.removeAll(newGameDoneList);
    }

    private void selectAndApplyActionAndStoreSearchStatistics(boolean render, boolean fastRuleLearning, int indexOfJustOneOfTheGames, List<Node> rootList, MCTS mcts, List<MinMaxStats> minMaxStatsList) {
        IntStream.range(0, gameList.size()).forEach(g ->
        {
            Game game = gameList.get(g);
            Node root = rootList.get(g);
            Action action;

            if (fastRuleLearning) {
                action = getRandomAction(root);
            } else {
                action = mcts.selectAction(root, minMaxStatsList.get(g));
            }
            game.apply(action);
            storeSearchStatistics(game, root, fastRuleLearning, minMaxStatsList == null ? new MinMaxStats(config.getKnownBounds()) : minMaxStatsList.get(g));


            if (render && indexOfJustOneOfTheGames != -1 && g == indexOfJustOneOfTheGames) {
                List<float[]> childVisitsList = justOneOfTheGames().getGameDTO().getPolicyTargets();
                float[] childVisits = childVisitsList.get(childVisitsList.size() - 1);
                justOneOfTheGames().renderMCTSSuggestion(config, childVisits);

                log.debug("\n" + game.render());
            }

        });
    }

    public void storeSearchStatistics(Game game, @NotNull Node root, boolean fastRuleLearning, MinMaxStats minMaxStats) {

        float[] policyTarget = new float[config.getActionSpaceSize()];
        if (fastRuleLearning) {
            root.getChildren().forEach((action, node) -> policyTarget[action.getIndex()] = (float) node.getPrior());
        } else {
            List<Pair<Action, Double>> distributionInput = regularizedPolicyOptimization.getDistributionInput(root, minMaxStats);
            for (Pair<Action, Double> e : distributionInput) {
                Action action = e.getKey();
                double v = e.getValue();
                policyTarget[action.getIndex()] = (float) v;
            }
        }
        game.getGameDTO().getPolicyTargets().add(policyTarget);
        game.getGameDTO().getRootValues().add((float) root.valueScore(minMaxStats, config));  // TODO check sign
        game.getGameDTO().getRootValuesFromInitialInference().add((float) root.getValueFromInitialInference());
    }

    @Nullable
    private List<NetworkIO> initialInference(Network network, boolean render, boolean fastRuleLearning, int indexOfJustOneOfTheGames) {
        inferenceDuration.value -= System.currentTimeMillis();
        List<NetworkIO> networkOutput = fastRuleLearning ? null : network.initialInferenceListDirect(gameList);
        inferenceDuration.value += System.currentTimeMillis();

        if (render && indexOfJustOneOfTheGames != -1) {
            justOneOfTheGames().renderNetworkGuess(config, justOneOfTheGames().toPlay(), Objects.requireNonNull(networkOutput).get(indexOfJustOneOfTheGames), false);
        }
        return networkOutput;
    }

    private void addExplorationNoise(boolean render, boolean explorationNoise, int indexOfJustOneOfTheGames, List<Node> rootList) {
        double fraction = config.getRootExplorationFraction();
        if (explorationNoise) {
            rootList.forEach(root -> addExplorationNoise(fraction, config.getRootDirichletAlpha(), root));
            if (render && indexOfJustOneOfTheGames != -1) {
                justOneOfTheGames().renderSuggestionFromPriors(config, rootList.get(indexOfJustOneOfTheGames));
            }
        }
    }

    @Nullable
    private List<MinMaxStats> runMCTS(Network network, boolean fastRuleLearning, List<Node> rootList, MCTS mcts) {
        return fastRuleLearning ? null :
                mcts.runParallel(rootList,
                        gameList.stream().map(Game::actionHistory).collect(Collectors.toList()),
                        network, inferenceDuration, config.getNumSimulations());
    }

    private void expandRootNodes(boolean fastRuleLearning, List<Node> rootList, List<NetworkIO> networkOutput) {
        IntStream.range(0, gameList.size()).forEach(g ->
        {
            NetworkIO networkIO = null;
            if (networkOutput != null) {
                networkIO = networkOutput.get(g);
            }
            mcts.expandNode(rootList.get(g),
                    gameList.get(g).toPlay(),
                    gameList.get(g).legalActions(),
                    networkIO, fastRuleLearning);
        });
    }

    @NotNull
    private List<Node> initRootNodes() {
        // At the root of the search tree we use the representation function to
        // obtain a hidden state given the current observation.
        return IntStream.range(0, gameList.size())
                .mapToObj(i -> new Node(config, 0, true))
                .collect(Collectors.toList());
    }

    public Game justOneOfTheGames() {
        return gameList.get(0);
    }

    public boolean notFinished() {
        return !gameList.isEmpty();
    }
}
