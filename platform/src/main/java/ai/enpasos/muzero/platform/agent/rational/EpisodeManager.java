package ai.enpasos.muzero.platform.agent.rational;

import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.rational.gumbel.SearchManager;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import umontreal.ssj.randvarmulti.DirichletGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@Slf4j
@Component
public class EpisodeManager {
    private static final RandomStream randomStreamBase = new MRG32k3a("rnd");
    private long start;
    private Duration inferenceDuration;
    private List<Game> gameList;
    private List<Game> gamesDoneList;

    @Autowired
    private MuZeroConfig config;


    @Autowired
    private MCTS mcts;

    public static Action getRandomAction(Node root) {
        List<Node> children = root.getChildren();
        if (children.size() == 0) {
            int i = 42;
        }
        int index = randomStreamBase.nextInt(0, children.size() - 1);
        return children.get(index).getAction();
    }


    private static double @NotNull [] numpyRandomDirichlet(double alpha, int dims) {

        double[] alphas = new double[dims];
        Arrays.fill(alphas, alpha);
        DirichletGen dg = new DirichletGen(randomStreamBase, alphas);
        double[] p = new double[dims];

        dg.nextPoint(p);
        return p;
    }

    public static double[] softmax(double[] raw) {
        double max = Arrays.stream(raw).max().getAsDouble();
        raw = Arrays.stream(raw).map(x -> x - max).toArray();
        double[] vs = Arrays.stream(raw).map(v -> Math.exp(v)).toArray();
        double sum = Arrays.stream(vs).sum();
        return Arrays.stream(vs).map(v -> v / sum).toArray();
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
        gameList.get(0).setDebug(true);
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

    public void play(Network network, boolean render, boolean fastRuleLearning) {
        int indexOfJustOneOfTheGames = getGameList().indexOf(justOneOfTheGames());

        shortCutForGamesWithoutAnOption(render);

        int nGames = this.gameList.size();
        if (nGames == 0) return;


        List<SearchManager> searchManagers = this.gameList.stream().map(game -> {
            game.initSearchManager();
            return game.getSearchManager();
        }).collect(Collectors.toList());

        List<NetworkIO> networkOutput = null;
        if (!fastRuleLearning) {
            //   List<Node> rootList = initRootNodes();
            networkOutput = initialInference(network, render, fastRuleLearning, indexOfJustOneOfTheGames);
        }
        List<NetworkIO> networkOutputFinal = networkOutput;

        IntStream.range(0, nGames).forEach(i -> {
            SearchManager sm = searchManagers.get(i);
            sm.expandRootNode(fastRuleLearning, fastRuleLearning ? null : networkOutputFinal.get(i));
            sm.gumbelActionsStart();

        });
        if (!fastRuleLearning) {
            do {
                List<List<Node>> searchPathList = new ArrayList<>();
                IntStream.range(0, nGames).forEach(i -> {
                    searchPathList.add(searchManagers.get(i).search());
                });

                if (inferenceDuration != null) inferenceDuration.value -= System.currentTimeMillis();
                List<NetworkIO> networkOutputList = mcts.recurrentInference(network, searchPathList);
                if (inferenceDuration != null) inferenceDuration.value += System.currentTimeMillis();

                IntStream.range(0, nGames).forEach(i -> {
                    searchManagers.get(i).expandAndBackpropagate(networkOutputList.get(i));
                    searchManagers.get(i).next();
                });

                // if there is an equal number of simulations in each game
                // the loop can be replaced by an explicit loop over the number of simulations
                // if the simulations are different per game the loop needs to be adjust
                // as the simulations would go on even for the game where simulation is over
            } while (searchManagers.stream().allMatch(sm -> !sm.isSimulationsFinished()));

        }
        IntStream.range(0, nGames).forEach(i ->
            searchManagers.get(i).selectAndApplyActionAndStoreSearchStatistics(render, fastRuleLearning)
        );


        renderNetworkGuess(network, render, indexOfJustOneOfTheGames);

        keepTrackOfOpenGames();
    }

    private void shortCutForGamesWithoutAnOption(boolean render) {
        List<Game> gamesWithOnlyOneAllowedAction = this.gameList.stream().filter(game -> game.legalActions().size() == 1).collect(Collectors.toList());
        if (gamesWithOnlyOneAllowedAction.size() == 0) return;


        this.gameList.removeAll(gamesWithOnlyOneAllowedAction);

        gamesWithOnlyOneAllowedAction.stream().forEach(game -> {
            Action action = game.legalActions().get(0);
            game.apply(action);

            float[] policyTarget = new float[config.getActionSpaceSize()];
            policyTarget[action.getIndex()] = 1f;

            game.getGameDTO().getPolicyTargets().add(policyTarget);
            if (game.getSearchManager() != null) {
                Node root = game.getSearchManager().getRoot();
                game.getGameDTO().getRootValuesFromInitialInference().add((float) root.getValueFromInitialInference());
            }

            if (render && game.isDebug()) {
                game.renderMCTSSuggestion(config, policyTarget);
                log.debug("\n" + game.render());
            }

        });

        this.gamesDoneList.addAll(gamesWithOnlyOneAllowedAction);

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


    @NotNull
    public List<Node> initRootNodes() {
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
