package ai.enpasos.muzero.platform.agent.rational.async.play;

import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.GameDTO;
import ai.enpasos.muzero.platform.agent.rational.*;
import ai.enpasos.muzero.platform.agent.rational.async.ModelService;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.add;
import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.sigmas;
//import static ai.enpasos.muzero.platform.agent.rational.SelfPlay.calculateSurprise;
import static ai.enpasos.muzero.platform.common.Functions.*;

@Component
@Slf4j
public class PlayAction {

    @Autowired
    MuZeroConfig config;


    @Autowired
    ModelService modelService;


    public void justReplayActionWithInitialInference(Game game) {
        log.trace("justReplayActionWithInitialInference");


        NetworkIO networkOutput = modelService.initialInference(game).join();

        Node root = new Node(config, 0, true);
        double value = Objects.requireNonNull(networkOutput).getValue();
        root.setValueFromInitialInference(value);
        game.getGameDTO().getRootValuesFromInitialInference().add((float) value);
        //calculateSurprise(value, game, config);

        int nActionsReplayed = game.actionHistory().getActionIndexList().size();
        if (nActionsReplayed < game.getOriginalGameDTO().getActions().size()) {
            int actionIndex = game.getOriginalGameDTO().getActions().get(nActionsReplayed);

            try {
                game.apply(actionIndex);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new MuZeroException(e);
            }

        }


    }

    @SuppressWarnings("squid:S3776")
    public void playAction(
        Game game,
        boolean render,
        boolean fastRuleLearning,
        boolean justInitialInferencePolicy,
        double pRandomActionRawAverage,
        boolean drawNotMaxWhenJustWithInitialInference
    ) {
        Action action;
        if (game.legalActions().size() == 1) {
            action = game.legalActions().get(game.legalActions().size() - 1);
            if (!fastRuleLearning) {
                NetworkIO networkOutput = modelService.initialInference(game).join();
                double value =  Objects.requireNonNull(networkOutput).getValue();
                game.getGameDTO().getRootValuesFromInitialInference().add((float) value);
                game.getGameDTO().getRootValueTargets().add((float)value);
            }
            float[] policyTarget = new float[config.getActionSpaceSize()];
            policyTarget[action.getIndex()] = 1f;
            game.getGameDTO().getPolicyTargets().add(policyTarget);
            if (!game.isReanalyse()) {
                game.getGameDTO().getPlayoutPolicy().add(policyTarget);
            }
            if (render && game.isDebug()) {
                game.renderMCTSSuggestion(config, policyTarget);
                log.info("\n" + game.render());
            }
            game.setActionApplied(true);

        } else {
            game.initSearchManager(pRandomActionRawAverage);
            GumbelSearch sm = game.getSearchManager();
            search(game, sm, fastRuleLearning, justInitialInferencePolicy, render);

            if (!justInitialInferencePolicy) {
                sm.storeSearchStatictics(render, fastRuleLearning);
            }

            boolean replay = false;
            action = selectAction(game, sm, fastRuleLearning, justInitialInferencePolicy, drawNotMaxWhenJustWithInitialInference, render, replay);// sm.selectAction( fastRuleLearning, replay);

        }
        applyAction(render, action, game, game.isDebug(), config);

        GameDTO dto = game.getGameDTO();
        if (dto.getPlayoutPolicy().size() < dto.getPolicyTargets().size()) {
            dto.getPlayoutPolicy().add( dto.getPolicyTargets().get( dto.getPolicyTargets().size() - 1));
        }

    }

    public static void applyAction(boolean render, Action action, Game game, boolean debug, MuZeroConfig config) {
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
    public void search(Game game, GumbelSearch sm, boolean fastRuleLearning, boolean justInitialInferencePolicy, boolean render) {
        log.trace("search");
        double value = 0;
        boolean withRandomness = false;
        NetworkIO networkOutput = null;
        if (!fastRuleLearning) {
            networkOutput = modelService.initialInference(game).join();
            value =  Objects.requireNonNull(networkOutput).getValue();
            //game.getGameDTO().getRootValueTargets().add((float)value);
            game.getGameDTO().getRootValuesFromInitialInference().add((float) value);
        }

        storeEntropyInfo(game, networkOutput);


//        if (!fastRuleLearning) {
//            calculateSurprise(value, game, config);
//        }



        if (justInitialInferencePolicy || game.legalActions().size() == 1) {
            expandRootNodeAfterJustWithInitialInference(sm, fastRuleLearning, game, networkOutput);
        } else {
            sm.expandRootNode(fastRuleLearning, fastRuleLearning ? null : Objects.requireNonNull(networkOutput));
        }
        if(!fastRuleLearning && game.isDebug() && render) {
            game.renderSuggestionFromPriors( config, sm.getRoot());
        }
        if (justInitialInferencePolicy || game.legalActions().size() == 1) {
            return;
        }


        if (!fastRuleLearning) sm.addExplorationNoise();
        sm.gumbelActionsStart(withRandomness);
        sm.drawCandidateAndAddValueStart();

        if (!fastRuleLearning) {
            do {
                List<Node> searchPath = sm.search();
                networkOutput = modelService.recurrentInference(searchPath).join();
                sm.expandAndBackpropagate(Objects.requireNonNull(networkOutput));
                sm.next();
                sm.drawCandidateAndAddValue();
            } while (!sm.isSimulationsFinished());
        }

    }


    public Action selectAction(Game game, GumbelSearch sm,  boolean fastRuleLearning, boolean justInitialInferencePolicy, boolean drawNotMaxWhenJustWithInitialInference, boolean render, boolean replay) {
        if (game.legalActions().size() == 1) {
            return game.legalActions().get(0);
        } else if (fastRuleLearning) {
            return sm.getRoot().getRandomAction();
        } else if (justInitialInferencePolicy) {
            return selectActionAfterJustWithInitialInference(sm.getRoot(), drawNotMaxWhenJustWithInitialInference);
        }
        else {
            return sm.selectAction( fastRuleLearning, replay);
        }
    }


    @Nullable
    private List<NetworkIO> initialInference(Network network, List<Game> gamesToApplyAction, boolean render, boolean fastRuleLearning, int indexOfJustOneOfTheGames) {
        List<NetworkIO> networkOutput = fastRuleLearning ? null : network.initialInferenceListDirect(gamesToApplyAction);
        return networkOutput;
    }


    private static void storeEntropyInfo(Game game, NetworkIO networkOutput) {
        List<Action> legalActions = game.legalActions();
        game.getGameDTO().getMaxEntropies().add((float) Math.log(legalActions.size()));
        if (networkOutput != null) {
            float[] ps = networkOutput.getPolicyValues();
            game.getGameDTO().getEntropies().add((float) entropy(toDouble(ps)));
        }
    }


    @SuppressWarnings({"squid:S3740", "unchecked"})
    private void expandRootNodeAfterJustWithInitialInference(GumbelSearch sm, boolean fastRuleLearning, Game game, NetworkIO networkOutput) {

        List<Action> legalActions = game.legalActions();
        Node root = sm.getRoot();

        root.expandRootNode(game.toPlay(), legalActions, networkOutput, fastRuleLearning);

    }


    @SuppressWarnings("squid:S3740")
    private Action selectActionAfterJustWithInitialInference(Node root, boolean drawNotMaxWhenJustWithInitialInference) {

        List<Pair<Action, Double>> distributionInput =
            root.getChildren().stream().map(node ->
                (Pair<Action, Double>) new Pair(node.getAction(), node.getPrior())
            ).collect(Collectors.toList());

        return drawNotMaxWhenJustWithInitialInference ? selectActionByDrawingFromDistribution(distributionInput)
            : selectActionByMaxFromDistribution(distributionInput);

    }


    public static void storeSearchResults(Game game, @NotNull Node root, boolean justPriorValues, MuZeroConfig config, Action selectedAction, MinMaxStats minMaxStats) {



        game.getGameDTO().getRootValueTargets().add((float) root.getImprovedValue());

        float[] policyTarget = new float[config.getActionSpaceSize()];
        if (justPriorValues) {
            root.getChildren().forEach(node -> policyTarget[node.getAction().getIndex()] = (float) node.getPrior());
        } else if (root.getChildren().size() == 1) {
            policyTarget[selectedAction.getIndex()] = 1f;
        } else {

            double[] logits = root.getChildren().stream().mapToDouble(node -> node.getGumbelAction().getLogit()).toArray();

            double[] completedQsNormalized = root.getCompletedQValuesNormalized(minMaxStats);

            int[] actions = root.getChildren().stream().mapToInt(node -> node.getAction().getIndex()).toArray();

            int maxActionVisitCount = 0;
            try {
                maxActionVisitCount = root.getChildren().stream().mapToInt(Node::getVisitCount).max().getAsInt();
            } catch (Exception e) {
                e.printStackTrace();
            }
            double[] raw = add(logits, sigmas(completedQsNormalized, maxActionVisitCount, config.getCVisit(), config.getCScale()));

            double[] improvedPolicy = softmax(raw);


            for (int i = 0; i < raw.length; i++) {
                int action = actions[i];
                double v = improvedPolicy[i];
                policyTarget[action] = (float) v;
            }
        }
        game.getGameDTO().getPolicyTargets().add(policyTarget);
    }


}
