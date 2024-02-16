package ai.enpasos.muzero.platform.agent.c_planning;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
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


    @Autowired
    GameBuffer gameBuffer;


    public void justReplayActionToGetRewardExpectations(Game game) {
        log.trace("justReplayActionToGetRewardExpectations");

        EpisodeDO episodeDO = game.getEpisodeDO();
        int t = game.getObservationInputTime();

        TimeStepDO timeStepDO = episodeDO.getTimeStep(t);

        NetworkIO networkOutput = modelService.initialInference(game).join();
        networkOutput = modelService.recurrentInference(networkOutput.getHiddenState(), timeStepDO.getAction()).join();

        double rewardExpectation = networkOutput.getReward();
        double r = timeStepDO.getReward() - rewardExpectation;
        double loss = r * r;
        timeStepDO.setRewardLoss((float)loss);

    }


    public void justReplayActionWithInitialInference(Game game) {
        log.trace("justReplayActionWithInitialInference");

        EpisodeDO episodeDO = game.getEpisodeDO();
        int t = episodeDO.getLastTimeWithAction() + 1;

        TimeStepDO timeStepDO = episodeDO.getTimeStep(t);

        NetworkIO networkOutput = modelService.initialInference(game).join();

        Node root = new Node(config, 0, true);
        double value = networkOutput.getValue();
        double reward = networkOutput.getReward();
        root.setValueFromInference(value);

        timeStepDO.setRootValueFromInitialInference((float) value);
        // TODO
      //  timeStepDO.setRewardFromInitialInference((float) value);

        if (t <= game.getOriginalEpisodeDO().getLastTimeWithAction()) {
            try {
                timeStepDO = episodeDO.getTimeStep(t);
                timeStepDO.setAction(game.getOriginalEpisodeDO().getTimeSteps().get(t).getAction());
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

        int t = game.getEpisodeDO().getLastTimeWithAction() + 1;
        TimeStepDO timeStepDO = game.getEpisodeDO().getTimeStep(t);

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

            timeStepDO.addLegalActions( legalActions);
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
            log.info("\nwith Dirichlet noise:");
            game.renderSuggestionFromPriors(config, sm.getRoot());
            //  log.info("\n" + game.render());
        }
        sm.gumbelActionsStart(withGumbel);
     //   sm.addValueStart();

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
           // log.info("\n" + game.render());
        }
        Action action = sm.selectAction(fastRuleLearning, replay, timeStepDO , gameBuffer.getPlanningBuffer().getEpisodeMemory());
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


        storeSearchStatistics(game, timeStepDO, root, true, config, null, new MinMaxStats(config.getKnownBounds()) );

        return action;
    }

    private static void storeEntropyInfo(Game game, TimeStepDO timeStepDO,  NetworkIO networkOutput) {
        List<Action> legalActions = game.legalActions();
        timeStepDO.setLegalActionMaxEntropy((float) Math.log(legalActions.size()));
        if (networkOutput != null) {
            float[] ps = networkOutput.getPolicyValues();
            timeStepDO.setEntropy((float) entropy(f2d(ps)));
        }
    }



}
