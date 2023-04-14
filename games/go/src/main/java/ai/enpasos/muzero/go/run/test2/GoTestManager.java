package ai.enpasos.muzero.go.run.test2;


import ai.enpasos.muzero.go.config.GoEnvironment;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.d_model.Inference;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.ZeroSumGame;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.common.Functions.selectActionByMaxFromDistribution;

@Slf4j
@Component
public class GoTestManager {


    @Autowired
    MuZeroConfig config;

    @Autowired
    Graph graph;

    @Autowired
    ModelService modelService;

    public void run(List<Integer> startPosition) {
        ZeroSumGame prototypeGame = (ZeroSumGame) config.newGame(true, true);
        prototypeGame.apply(startPosition);
        graph.init(StateKey.getFrom(prototypeGame));

        loadLatestModel();


        int c = 0;
        while(!graph.getRoot().isReady(graph.perspective)) {
            ZeroSumGame game = (ZeroSumGame)prototypeGame.copy();
            runEpisode(game);
            System.out.println("episode " + ++c + ", nodes: " + graph.allNodes.size());
        }

    }

    private void loadLatestModel() {
        try {
            modelService.loadLatestModel().get();
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
            Thread.interrupted();
        } catch (Exception e) {
            throw new MuZeroException(e);
        }
    }

    private void runEpisode(ZeroSumGame game) {

        GNode node = graph.getRoot();
        while(!node.isReady(graph.perspective) && !game.legalActions().isEmpty()) {
            GNode nodeFinal = node;
            List<Action> relevantActions = game.legalActions().stream()
                    .filter(action -> !nodeFinal.getOutRelations().stream().anyMatch(r -> r.action == action.getIndex()) ||
                                      nodeFinal.getOutRelations().stream().anyMatch(r -> r.action == action.getIndex() && r.to.isReady(graph.perspective))
                             )
                    .collect(Collectors.toList());
            if (relevantActions.isEmpty()) {
                break;
            }

            Action action = selectAnAction(relevantActions, game);


            node = expand(graph, game, action, false).getSecond();
        }
        backpropagate(game, node) ;
    }

    private   Action selectAnAction(List<Action> relevantActions, Game game) {
        //Action action;
// best in parallel

        List<NetworkIO> networkOutputList = null;
        try {
            networkOutputList = modelService.initialInference(List.of(game)).get();
        } catch (  ExecutionException e) {
            throw new MuZeroException(e);
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
            Thread.interrupted();
        }

        int g = 0;

     //   List<Action> legalActions = game.legalActions();
        float[] policyValues = Objects.requireNonNull(networkOutputList).get(g).getPolicyValues();
        List<Pair<Action, Double>> distributionInput =
                IntStream.range(0, game.getConfig().getActionSpaceSize())
                        .filter(i -> {
                            Action action = game.getConfig().newAction(i);
                            return relevantActions.contains(action);
                        })
                        .mapToObj(i -> {
                            Action action = game.getConfig().newAction(i);
                            double v = policyValues[i];
                            return new Pair<>(action, v);
                        }).collect(Collectors.toList());

        Action action = selectActionByMaxFromDistribution(distributionInput);
        return   action;


    }

    private void backpropagate(ZeroSumGame game, GNode node ) {
        int v = 0;
        if (game.legalActions().isEmpty()) {
            if (game.getEnvironment().hasPlayerWon(OneOfTwoPlayer.PLAYER_A)) {
                v = 1;
            }
            if (game.getEnvironment().hasPlayerWon(OneOfTwoPlayer.PLAYER_B)) {
                v = -1;
            }
            if (graph.perspective == -1) {
                v = -v;
            }
        } else if (node.isReady(graph.perspective)) {
            v = node.value;
        }

       for (GRelation r : node.getInRelations()) {
         GNode parent =  r.from;
          if (parent != null) {
              parent.backpropagate(-v,  graph.perspective);
          }
       }



    }

    private Pair<ZeroSumGame, GNode> expand(Graph graph, ZeroSumGame startGame, Action action, boolean createNewGame) {
        StateKey stateKey = StateKey.getFrom(startGame);
        GNode node = graph.getNode(stateKey);
        ZeroSumGame game = createNewGame ?   (ZeroSumGame) startGame.copy() : startGame;
        game.apply(action);
        graph.applyAction(node, action.getIndex(), StateKey.getFrom(game));
        node.setExpanded(true);
        return Pair.create(game, node);
    }


    private void expand(Graph graph, Game startGame) {
        GNode node = graph.getNode(StateKey.getFrom(startGame));
        startGame.legalActions().forEach(action -> {
            ZeroSumGame newGame = (ZeroSumGame) startGame.copy();
            newGame.apply(action);
            graph.applyAction(node, action.getIndex(), StateKey.getFrom(newGame));
        });
        node.setExpanded(true);
    }
}
