package ai.enpasos.muzero.platform.agent.c_planning;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.platform.agent.c_planning.GumbelSearch.storeSearchStatistics;
import static ai.enpasos.muzero.platform.common.Functions.*;

@SuppressWarnings("unchecked")
@Component
@Slf4j
public class PlanAction {

    @Autowired
    MuZeroConfig config;


    @Autowired
    ModelService modelService;


    public void justReplayActionWithInitialInference(Game game) {
        log.trace("justReplayActionWithInitialInference");

        EpisodeDO episodeDO = game.getEpisodeDO();
        int t = episodeDO.getLastTimeWithAction() + 1;
//        if (t == -1) {
//            int i = 42;
//        }
        TimeStepDO timeStepDO = episodeDO.getTimeSteps().get(t);

        NetworkIO networkOutput = modelService.initialInference(game).join();

        Node root = new Node(config, 0, true);
        double value = networkOutput.getValue();
        root.setValueFromInference(value);
        double entropyValue = networkOutput.getEntropyValue();
        root.setEntropyValueFromInference(entropyValue);


        timeStepDO.setRootValueFromInitialInference((float) value);
        timeStepDO.setRootEntropyValueFromInitialInference((float) entropyValue);

      //  int nActionsReplayed = t;
        if (t < game.getOriginalEpisodeDO().getLastTime() ) {

            try {
                timeStepDO = episodeDO.getTimeSteps().get(t);
                timeStepDO.setAction(game.getOriginalEpisodeDO().getTimeSteps().get(t).getAction());

                episodeDO.addNewTimeStepDO();

                timeStepDO = episodeDO.getTimeSteps().get(t+1);
                timeStepDO.setObservation(game.getOriginalEpisodeDO().getTimeSteps().get(t+1).getObservation());
                  } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new MuZeroException(e);
            }

        }


    }


    public Action planAction(
            Game game,
            boolean render,
            boolean fastRuleLearning,
            boolean justInitialInferencePolicy,
            double pRandomActionRawAverage,
            boolean replay,
            boolean withGumbel
    ) {
                if (render && game.isDebug()) {
            log.info("\n" + game.render());
        }
        NetworkIO networkOutput = null;
        if (!fastRuleLearning) {
            networkOutput = modelService.initialInference(game).join();
        }

      //  EpisodeDO episodeDO = game.getEpisodeDO();
      //  int t = episodeDO.getLastTimeStep().orElseThrow().getT();
      //  TimeStepDO timeStepDO = episodeDO.getTimeSteps().get(t);
        TimeStepDO timeStepDO = game.getEpisodeDO().getLastTimeStep();

                storeEntropyInfo(game, timeStepDO, networkOutput );
        if (justInitialInferencePolicy) {
            return playAfterJustWithInitialInference(timeStepDO, fastRuleLearning, game , networkOutput );
        }



        if (!fastRuleLearning) {
            double value = networkOutput .getValue();
            timeStepDO.setRootValueFromInitialInference((float) value);
        }
        if (!replay) {
            boolean[] legalActions = new boolean[config.getActionSpaceSize()];
            for (Action action : game.legalActions()) {
                legalActions[action.getIndex()] = true;
            }
           // game.getGameDTO().getLegalActions().add(legalActions);
        }
        if (game.legalActions().size() == 1) {
             return shortCutForGameWithoutAnOption(game,   timeStepDO, networkOutput, render, fastRuleLearning, replay);
        }

        game.initSearchManager(pRandomActionRawAverage);
        GumbelSearch sm = game.getSearchManager();

        sm.expandRootNode(fastRuleLearning, fastRuleLearning ? null : networkOutput);
        if (render && game.isDebug()) {
            game.renderSuggestionFromPriors(config, sm.getRoot());
          //  log.info("\n" + game.render());
        }
        if (!fastRuleLearning) sm.addExplorationNoise();
        if (render && game.isDebug()) {
            game.renderSuggestionFromPriors(config, sm.getRoot());
            //  log.info("\n" + game.render());
        }
        sm.gumbelActionsStart(withGumbel);
        sm.drawCandidateAndAddValueStart();

        if (!fastRuleLearning && !sm.isSimulationsFinished()) {
            do {
                List<Node> searchPath = sm.search();
                try {
                    networkOutput = modelService.recurrentInference(searchPath).get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
                sm.expand(networkOutput);
                sm.backpropagate(networkOutput, config.getDiscount());
                sm.next();
                sm.drawCandidateAndAddValue();
            } while (!sm.isSimulationsFinished());
        }
        sm.storeSearchStatictics(fastRuleLearning, timeStepDO);
        if (render && game.isDebug()) {
            game.renderMCTSSuggestion(config, timeStepDO.getPolicyTarget() );
            log.info("\n" + game.render());
        }
        Action action = sm.selectAction(fastRuleLearning, replay, timeStepDO);
        return action;
    }




    private Action shortCutForGameWithoutAnOption(Game game,TimeStepDO timeStepDO, NetworkIO networkOutput, boolean render, boolean fastRuleLearning, boolean replay) {

        Action action = game.legalActions().get(0);

        float value = 0f;
        if (!fastRuleLearning) {
            value = (float) networkOutput.getValue();
        }

        timeStepDO.setRootValueTarget(value);
        timeStepDO.setVMix(value);

        float[] policyTarget = new float[config.getActionSpaceSize()];
        policyTarget[action.getIndex()] = 1f;
        timeStepDO.setPolicyTarget(policyTarget);
        if (!replay) {
            timeStepDO.setPlayoutPolicy(policyTarget);
        }
        if (render && game.isDebug()) {
            game.renderMCTSSuggestion(config, policyTarget);
            log.info("\n" + game.render());
        }
        game.setActionApplied(true);
        return action;
    }

    @SuppressWarnings({"squid:S3740", "unchecked"})
    private Action playAfterJustWithInitialInference(TimeStepDO timeStepDO, boolean fastRuleLearning,  Game game,  NetworkIO  networkOutput ) {


        List<Action> legalActions = game.legalActions();
        Node root = new Node(config, 0, true);

        root.expandRootNode(game.toPlay(), legalActions, networkOutput , fastRuleLearning);

        List<Pair<Action, Double>> distributionInput =
                root.getChildren().stream().map(node ->
                        (Pair<Action, Double>) new Pair(node.getAction(), node.getPrior())
                ).collect(Collectors.toList());

        Action action = selectActionByDrawingFromDistribution(distributionInput);


        storeSearchStatistics(game, timeStepDO, root, true, config, null, new MinMaxStats(config.getKnownBounds()),  new MinMaxStats(config.getKnownBounds()));

        return action;
    }

    private static void storeEntropyInfo(Game game, TimeStepDO timeStepDO,  NetworkIO networkOutput) {
        List<Action> legalActions = game.legalActions();
        timeStepDO.setLegalActionMaxEntropy((float) Math.log(legalActions.size()));
        if (networkOutput != null) {
            float[] ps = networkOutput.getPolicyValues();
            timeStepDO.setEntropy((float) entropy(toDouble(ps)));
        }
    }


//    @SuppressWarnings("squid:S3776")
//    public Action planActionOld(
//        Game game,
//        boolean render,
//        boolean fastRuleLearning,
//        boolean justInitialInferencePolicy,
//        double pRandomActionRawAverage,
//        boolean drawNotMaxWhenJustWithInitialInference
//    ) {
//        if (render && game.isDebug()) {
//            log.info("\n" + game.render());
//        }
//        Action action;
//        if (game.legalActions().size() == 1) {
//            action = game.legalActions().get(game.legalActions().size() - 1);
//            if (!fastRuleLearning) {
//                NetworkIO networkOutput = modelService.initialInference(game).join();
//
//                double value =   networkOutput.getValue();
//                game.getGameDTO().getRootValuesFromInitialInference().add((float) value);
//                game.getGameDTO().getRootValueTargets().add((float)value);
//
//                double entropyValue = networkOutput.getEntropyValue();
//                game.getGameDTO().getRootEntropyValuesFromInitialInference().add((float) entropyValue);
//                game.getGameDTO().getRootEntropyValueTargets().add((float)entropyValue);
//
//            }  else {
//                double value =   0;
//                game.getGameDTO().getRootValuesFromInitialInference().add((float) value);
//                game.getGameDTO().getRootValueTargets().add((float)value);
//
//                double entropyValue = 0;  // just some value
//                game.getGameDTO().getRootEntropyValuesFromInitialInference().add((float) entropyValue);
//                game.getGameDTO().getRootEntropyValueTargets().add((float)entropyValue);
//            }
//            game.getGameDTO().getLegalActionMaxEntropies().add(0f);
//            game.getGameDTO().getEntropies().add(0f);
//            float[] policyTarget = new float[config.getActionSpaceSize()];
//            policyTarget[action.getIndex()] = 1f;
//            game.getGameDTO().getPolicyTargets().add(policyTarget);
//            if (!game.isReanalyse()) {
//                game.getGameDTO().getPlayoutPolicy().add(policyTarget);
//            }
//
//        } else {
//            game.initSearchManager(pRandomActionRawAverage);
//            GumbelSearch sm = game.getSearchManager();
//            search(game, sm, fastRuleLearning, justInitialInferencePolicy, render);
//
//            if (!justInitialInferencePolicy) {
//                sm.storeSearchStatictics( fastRuleLearning);
//            }
//
//            boolean replay = game.isReanalyse();
//            action = selectAction(game, sm, fastRuleLearning, justInitialInferencePolicy, drawNotMaxWhenJustWithInitialInference, replay);
//        }
//
//        if (action == null) {
//            throw new MuZeroException("action must not be null");
//        }
//
//
//        if (render && game.isDebug()) {
//            List<float[]> policyTargets = game.getGameDTO().getPolicyTargets();
//            float[] policyTarget = policyTargets.get(policyTargets.size() - 1);
//            game.renderMCTSSuggestion(config, policyTarget);
//
//            if (!game.isReanalyse()) {
//                List<float[]> playoutPolicys = game.getGameDTO().getPlayoutPolicy();
//                if (playoutPolicys.size() > 0) {
//                    float[] playoutPolicy = playoutPolicys.get(playoutPolicys.size() - 1);
//                    game.renderMCTSSuggestion(config, playoutPolicy);
//                }
//            }
//        }
//
//        GameDTO dto = game.getGameDTO();
//        if (dto.getPlayoutPolicy().size() < dto.getPolicyTargets().size()) {
//            dto.getPlayoutPolicy().add( dto.getPolicyTargets().get( dto.getPolicyTargets().size() - 1));
//        }
//        return action;
//
//    }


//    public void search(Game game, GumbelSearch sm, boolean fastRuleLearning, boolean justInitialInferencePolicy, boolean render) {
//        log.trace("search");
//
//        boolean withRandomness = false;
//        NetworkIO networkOutput = null;
//        if (!fastRuleLearning) {
//            networkOutput = modelService.initialInference(game).join();
//            double value =  Objects.requireNonNull(networkOutput).getValue();
//            game.getGameDTO().getRootValuesFromInitialInference().add((float) value);
//
//            double entropyValue = networkOutput.getEntropyValue();
//            game.getGameDTO().getRootEntropyValuesFromInitialInference().add((float) entropyValue);
//        }
//
//        if (justInitialInferencePolicy || game.legalActions().size() == 1) {
//            expandRootNodeAfterJustWithInitialInference(sm, fastRuleLearning, game, networkOutput);
//        } else {
//            sm.expandRootNode(fastRuleLearning, fastRuleLearning ? null : Objects.requireNonNull(networkOutput));
//        }
//        storeEntropyInfo(game, sm.getRoot(), networkOutput);
//
//
//        if (justInitialInferencePolicy || game.legalActions().size() == 1) {
//            if(!fastRuleLearning && game.isDebug() && render) {
//                game.renderSuggestionFromPriors( config, sm.getRoot());
//            }
//            return;
//        }
//
//
//        if (!fastRuleLearning) sm.addExplorationNoise();
//
//        if(!fastRuleLearning && game.isDebug() && render) {
//            game.renderSuggestionFromPriors( config, sm.getRoot());
//        }
//
//
//
//        sm.gumbelActionsStart(withRandomness);
//
//        if (!fastRuleLearning) {
//            do {
//                List<Node> searchPath = sm.search();
//                networkOutput = modelService.recurrentInference(searchPath).join();
//                sm.expand(Objects.requireNonNull(networkOutput));
//                sm.backpropagate(networkOutput, this.config.getDiscount());
//                sm.next();
//                sm.drawCandidateAndAddValue();
//            } while (!sm.isSimulationsFinished());
//        }
//
//    }


//    public Action selectAction(Game game, GumbelSearch sm, boolean fastRuleLearning, boolean justInitialInferencePolicy, boolean drawNotMaxWhenJustWithInitialInference, boolean replay) {
//        if (game.legalActions().size() == 1) {
//            return game.legalActions().get(0);
//        } else if (fastRuleLearning) {
//            return sm.getRoot().getRandomAction();
//        } else if (justInitialInferencePolicy) {
//            return selectActionAfterJustWithInitialInference(sm.getRoot(), drawNotMaxWhenJustWithInitialInference);
//        }
//        else {
//            return sm.selectAction( fastRuleLearning, replay);
//        }
//    }



//    private static void storeEntropyInfo(Game game, Node node, NetworkIO networkOutput) {
//        List<Action> legalActions = game.legalActions();
//        game.getGameDTO().getLegalActionMaxEntropies().add((float) Math.log(legalActions.size()));
//        double entropy = 0f;
//        if (networkOutput != null) {
//            entropy = networkOutput.getEntropyFromPolicyValues();
//        }
//        node.setEntropy(entropy);
//        game.getGameDTO().getEntropies().add((float)  entropy);
//
////        if (!node.getChildren().isEmpty()) {
////            double[] ps = node.getChildren().stream().mapToDouble(Node::getPrior).toArray();
////            double entropy = entropy(ps);
////            node.setEntropy(entropy);
////            game.getGameDTO().getEntropies().add((float)  entropy);
////        }
//    }


//    @SuppressWarnings({"squid:S3740", "unchecked"})
//    private void expandRootNodeAfterJustWithInitialInference(GumbelSearch sm, boolean fastRuleLearning, Game game, NetworkIO networkOutput) {
//
//        List<Action> legalActions = game.legalActions();
//        Node root = sm.getRoot();
//
//        root.expandRootNode(game.toPlay(), legalActions, networkOutput, fastRuleLearning);
//
//    }
//
//
//    @SuppressWarnings("squid:S3740")
//    private Action selectActionAfterJustWithInitialInference(Node root, boolean drawNotMaxWhenJustWithInitialInference) {
//
//        List<Pair<Action, Double>> distributionInput =
//            root.getChildren().stream().map(node ->
//                (Pair<Action, Double>) new Pair(node.getAction(), node.getPrior())
//            ).collect(Collectors.toList());
//
//        return drawNotMaxWhenJustWithInitialInference ? selectActionByDrawingFromDistribution(distributionInput)
//            : selectActionByMaxFromDistribution(distributionInput);
//
//    }
//
//
//    public static void storeSearchResults(Game game, @NotNull Node root, boolean justPriorValues, MuZeroConfig config, Action selectedAction, MinMaxStats minMaxStats, MinMaxStats minMaxStatsEntropyQValues) {
//
//
//
//        game.getGameDTO().getRootValueTargets().add((float) root.getImprovedValue());
//
//        float[] policyTarget = new float[config.getActionSpaceSize()];
//        if (justPriorValues) {
//            root.getChildren().forEach(node -> policyTarget[node.getAction().getIndex()] = (float) node.getPrior());
//        } else if (root.getChildren().size() == 1) {
//            policyTarget[selectedAction.getIndex()] = 1f;
//        } else {
//
//            double[] logits = root.getChildren().stream().mapToDouble(node -> node.getGumbelAction().getLogit()).toArray();
//
//            double[] completedQsNormalized = root.getCompletedQValuesNormalized(minMaxStats);
//            double[] completedEntropyQsNormalized = root.getCompletedQEntropyValuesNormalized(minMaxStatsEntropyQValues);
//
//            int[] actions = root.getChildren().stream().mapToInt(node -> node.getAction().getIndex()).toArray();
//
//            int maxActionVisitCount = 0;
//            try {
//                maxActionVisitCount = root.getChildren().stream().mapToInt(Node::getVisitCount).max().getAsInt();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//
//            double[] raw = add(logits, sigmas(
//                    game.isItExplorationTime() ?  add(completedQsNormalized, completedEntropyQsNormalized) : completedQsNormalized
//                    , maxActionVisitCount, config.getCVisit(), config.getCScale()));
//
//            double[] improvedPolicy = softmax(raw);
//
//
//            for (int i = 0; i < raw.length; i++) {
//                int action = actions[i];
//                double v = improvedPolicy[i];
//                policyTarget[action] = (float) v;
//            }
//        }
//        game.getGameDTO().getPolicyTargets().add(policyTarget);
//    }


}
