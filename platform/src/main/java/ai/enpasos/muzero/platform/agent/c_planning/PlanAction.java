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
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Time;
import java.util.Arrays;
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


    public void justReplayActionWithInitialInference(Game game) {
        log.trace("justReplayActionWithInitialInference");

        EpisodeDO episodeDO = game.getEpisodeDO();
        int t = episodeDO.getLastTimeWithAction() + 1;

        TimeStepDO timeStepDO = episodeDO.getTimeStep(t);

        NetworkIO networkOutput = modelService.initialInference(game).join();

        Node root = new Node(config, 0, true);
        double value = networkOutput.getValue();
        root.setValueFromInference(value);



        timeStepDO.setRootValueFromInitialInference((float) value);


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


        Action biasedAction = null;

        if (!fastRuleLearning && !sm.isSimulationsFinished()) {
            do {
                List<Node> searchPath = sm.search();

//                String as = Arrays.toString(searchPath.stream().filter(n -> n.getAction() != null).mapToInt(n -> n.getAction().getIndex()).toArray());
//                System.out.println("search path: " + as   );
//                if (as.equals("[1, 0]")) {
//                    int i = 42;
//                }

                try {
                    networkOutput = modelService.recurrentInference(searchPath).get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }

                double oneKindOfExpectedSurprise = networkOutput.entropyOfLegalValues();

// 100 is blocking the shortcut, .... check the idea
                if ( config.getPlayTypeKey() ==  PlayTypeKey.HYBRID && oneKindOfExpectedSurprise > 100.0) {
                    System.out.println("oneKindOfExpectedSurprise: " + oneKindOfExpectedSurprise);
                    // the higher the entropy, the more the expected new information when reaching
                   // this state in the environment, therefore do a shortcut here
                   Action action = searchPath.get(1).getAction();
                    biasedAction = action;
                  // action.setSwitchToExploration(true);

                    // the selected action is now under exploration policy, therefore we need to
//                    game.getEpisodeDO().setHybrid(true);
//                   if (game.getEpisodeDO().getTStartNormal()<=t) {
//                       game.getEpisodeDO().setTStartNormal(t+1);
//                   }
//                  // TimeStepDO ts = game.getEpisodeDO().getLastTimeStep();
//                   timeStepDO.setPolicyTarget(new float[config.getActionSpaceSize()]);
//                    timeStepDO.setPlayoutPolicy(new float[config.getActionSpaceSize()]);
//                    timeStepDO.getPlayoutPolicy()[action.getIndex()] = 1f;
//                    game.setMarker(true);
//                   return action;
                }

                boolean debug = false;
                if (debug) {
                    int[] actions = searchPath.stream().map(Node::getAction).filter(a -> a != null).mapToInt(Action::getIndex).toArray();
                    log.info("{}: v={}, p={}", Arrays.toString(actions), networkOutput.getValue(), Arrays.toString(networkOutput.getPolicyValues()));
                }
                boolean expanded = sm.expand(networkOutput);
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
        Action action = sm.selectAction(biasedAction, fastRuleLearning, replay, timeStepDO, game.isHybrid2(), gameBuffer.getBuffer().getEpisodeMemory());
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
