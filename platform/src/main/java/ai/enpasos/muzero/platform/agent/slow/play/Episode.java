package ai.enpasos.muzero.platform.agent.slow.play;

import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.NetworkIO;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import umontreal.ssj.randvarmulti.DirichletGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@Slf4j
public class Episode {
    private static final RandomStream randomStreamBase = new MRG32k3a("rnd");
    private long start;
    private Duration inferenceDuration;
    private List<Game> gameList;
    private List<Game> gamesDoneList;

    // Episode played for a number of config.getNumParallelPlays() games in parallel
    public Episode(MuZeroConfig config) {
        start = System.currentTimeMillis();
        inferenceDuration = new Duration();
        gameList = IntStream.rangeClosed(1, config.getNumParallelPlays())
                .mapToObj(i -> config.newGame())
                .collect(Collectors.toList());
        gamesDoneList = new ArrayList<>();
    }

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
            child.prior = (1 - rootExplorationFraction) * child.prior + rootExplorationFraction * noise[i];
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

    public void play(@NotNull MuZeroConfig config, Network network, boolean render, boolean fastRuleLearning, boolean explorationNoise) {
        int indexOfJustOneOfTheGames = getGameList().indexOf(justOneOfTheGames());

        // At the root of the search tree we use the representation function to
        // obtain a hidden state given the current observation.
        List<Node> rootList = IntStream.range(0, gameList.size())
                .mapToObj(i -> new Node(config, 0, true))
                .collect(Collectors.toList());

        inferenceDuration.value -= System.currentTimeMillis();

        List<NetworkIO> networkOutput = fastRuleLearning ? null : network.initialInferenceListDirect(gameList);

        inferenceDuration.value += System.currentTimeMillis();

        if (render && indexOfJustOneOfTheGames != -1) {
            justOneOfTheGames().renderNetworkGuess(config, justOneOfTheGames().toPlay(), Objects.requireNonNull(networkOutput).get(indexOfJustOneOfTheGames), false);
        }

        MCTS mcts = new MCTS(config);

        IntStream.range(0, gameList.size()).forEach(g ->
            {
                NetworkIO networkIO = null;
                if (networkOutput != null) {
                    networkIO = networkOutput.get(g);
                }
                mcts.expandNode(rootList.get(g),
                        gameList.get(g).toPlay(),
                        gameList.get(g).legalActions(),
                        networkIO, fastRuleLearning, config);
            });

        double fraction = config.getRootExplorationFraction();
        if (explorationNoise) {
            rootList.forEach(root -> addExplorationNoise(fraction, config.getRootDirichletAlpha(), root));

            if (render && indexOfJustOneOfTheGames != -1) {
                justOneOfTheGames().renderSuggestionFromPriors(config, rootList.get(indexOfJustOneOfTheGames));
            }
        }
        final List<MinMaxStats> minMaxStatsList = fastRuleLearning? null :
                mcts.runParallel(rootList,
                    gameList.stream().map(Game::actionHistory).collect(Collectors.toList()),
                    network, inferenceDuration, config.getNumSimulations());


        IntStream.range(0, gameList.size()).forEach(g ->
        {
            Game game = gameList.get(g);
            Node root = rootList.get(g);
            Action action = null;

            if (fastRuleLearning) {
                action = getRandomAction(root);
            } else {
                action = mcts.selectAction(root, minMaxStatsList.get(g));
            }
            game.apply(action);
            game.storeSearchStatistics(root, fastRuleLearning, minMaxStatsList == null ? new MinMaxStats(config.getKnownBounds()) : minMaxStatsList.get(g));


            if (render && indexOfJustOneOfTheGames != -1 && g == indexOfJustOneOfTheGames) {
                List<float[]> childVisitsList = justOneOfTheGames().getGameDTO().getPolicyTarget();
                float[] childVisits = childVisitsList.get(childVisitsList.size() - 1);
                justOneOfTheGames().renderMCTSSuggestion(config, childVisits);

                log.debug("\n" + game.render());
            }

        });

        List<Game> newGameDoneList = gameList.stream()
                .filter(game -> game.terminal() || game.getGameDTO().getActionHistory().size() >= config.getMaxMoves())
                .collect(Collectors.toList());

        if (render && indexOfJustOneOfTheGames != -1 && justOneOfTheGames().terminal()) {
            NetworkIO networkOutput2 = network.initialInferenceDirect(gameList.get(indexOfJustOneOfTheGames));
            justOneOfTheGames().renderNetworkGuess(config, justOneOfTheGames().toPlay(), networkOutput2, true);
        }

        gamesDoneList.addAll(newGameDoneList);
        gameList.removeAll(newGameDoneList);
    }

    public Game justOneOfTheGames() {
        return gameList.get(0);
    }

    public boolean notFinished() {
        return !gameList.isEmpty();
    }
}
