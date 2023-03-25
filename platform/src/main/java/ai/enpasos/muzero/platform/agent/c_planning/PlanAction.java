package ai.enpasos.muzero.platform.agent.c_planning;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameDTO;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.platform.agent.c_planning.GumbelFunctions.add;
import static ai.enpasos.muzero.platform.agent.c_planning.GumbelFunctions.sigmas;
//import static ai.enpasos.muzero.platform.agent.rational.SelfPlay.calculateSurprise;
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


        NetworkIO networkOutput = modelService.initialInference(game).join();

        Node root = new Node(config, 0, true);
        double value = networkOutput.getValue();
        root.setValueFromInference(value);
        double entropyValue = networkOutput.getEntropyValue();
        root.setEntropyValueFromInference(entropyValue);


        game.getGameDTO().getRootValuesFromInitialInference().add((float) value);
        game.getGameDTO().getRootEntropyValuesFromInitialInference().add((float) entropyValue);
        //calculateSurprise(value, game, config);

        int nActionsReplayed = game.getGameDTO().getActions().size();
        if (nActionsReplayed < game.getOriginalGameDTO().getActions().size()) {
            int actionIndex = game.getOriginalGameDTO().getActions().get(nActionsReplayed);

            try {
                //game.apply(actionIndex);
                game.getGameDTO().getActions().add(game.getOriginalGameDTO().getActions().get(nActionsReplayed));
                game.getGameDTO().getObservations().add(game.getOriginalGameDTO().getObservations().get(1+nActionsReplayed));

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new MuZeroException(e);
            }

        }


    }

    @SuppressWarnings("squid:S3776")
    public Action planAction(
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

                double value =   networkOutput.getValue();
                game.getGameDTO().getRootValuesFromInitialInference().add((float) value);
                game.getGameDTO().getRootValueTargets().add((float)value);

                double entropyValue = networkOutput.getEntropyValue();
                game.getGameDTO().getRootEntropyValuesFromInitialInference().add((float) entropyValue);
                game.getGameDTO().getRootEntropyValueTargets().add((float)entropyValue);

            }  else {
                double value =   0;
                game.getGameDTO().getRootValuesFromInitialInference().add((float) value);
                game.getGameDTO().getRootValueTargets().add((float)value);

                double entropyValue = 0;  // just some value
                game.getGameDTO().getRootEntropyValuesFromInitialInference().add((float) entropyValue);
                game.getGameDTO().getRootEntropyValueTargets().add((float)entropyValue);
            }
            game.getGameDTO().getMaxEntropies().add(0f);
            game.getGameDTO().getEntropies().add(0f);
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
        } else {
            game.initSearchManager(pRandomActionRawAverage);
            GumbelSearch sm = game.getSearchManager();
            search(game, sm, fastRuleLearning, justInitialInferencePolicy, render);

            if (!justInitialInferencePolicy) {
                sm.storeSearchStatictics( fastRuleLearning);
            }

            boolean replay = game.isReanalyse();
            action = selectAction(game, sm, fastRuleLearning, justInitialInferencePolicy, drawNotMaxWhenJustWithInitialInference, render, replay);
        }

        if (action == null) {
            throw new MuZeroException("action must not be null");
        }


        if (render && game.isDebug()) {
            List<float[]> policyTargets = game.getGameDTO().getPolicyTargets();
            float[] policyTarget = policyTargets.get(policyTargets.size() - 1);
            game.renderMCTSSuggestion(config, policyTarget);
            log.info("\n" + game.render());
        }

        GameDTO dto = game.getGameDTO();
        if (dto.getPlayoutPolicy().size() < dto.getPolicyTargets().size()) {
            dto.getPlayoutPolicy().add( dto.getPolicyTargets().get( dto.getPolicyTargets().size() - 1));
        }
        return action;

    }


    public void search(Game game, GumbelSearch sm, boolean fastRuleLearning, boolean justInitialInferencePolicy, boolean render) {
        log.trace("search");

        boolean withRandomness = false;
        NetworkIO networkOutput = null;
        if (!fastRuleLearning) {
            networkOutput = modelService.initialInference(game).join();
            double value =  Objects.requireNonNull(networkOutput).getValue();
            game.getGameDTO().getRootValuesFromInitialInference().add((float) value);

            double entropyValue = networkOutput.getEntropyValue();
            game.getGameDTO().getRootEntropyValuesFromInitialInference().add((float) entropyValue);
        }

        if (justInitialInferencePolicy || game.legalActions().size() == 1) {
            expandRootNodeAfterJustWithInitialInference(sm, fastRuleLearning, game, networkOutput);
        } else {
            sm.expandRootNode(fastRuleLearning, fastRuleLearning ? null : Objects.requireNonNull(networkOutput));
        }
        storeEntropyInfo(game, sm.getRoot());

        if(!fastRuleLearning && game.isDebug() && render) {
            game.renderSuggestionFromPriors( config, sm.getRoot());
        }
        if (justInitialInferencePolicy || game.legalActions().size() == 1) {
            return;
        }


        if (!fastRuleLearning) sm.addExplorationNoise();
        sm.gumbelActionsStart(withRandomness);
      //  sm.drawCandidateAndAddValueStart();

        if (!fastRuleLearning) {
            do {
                List<Node> searchPath = sm.search();
                networkOutput = modelService.recurrentInference(searchPath).join();
                sm.expand(Objects.requireNonNull(networkOutput));
                sm.backpropagate(networkOutput, this.config.getDiscount());
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



    private static void storeEntropyInfo(Game game, Node node) {
        List<Action> legalActions = game.legalActions();
        game.getGameDTO().getMaxEntropies().add((float) Math.log(legalActions.size()));
        if (!node.getChildren().isEmpty()) {
            double[] ps = node.getChildren().stream().mapToDouble(n -> n.getPrior()).toArray();
            double entropy = entropy(ps);
            node.setEntropy(entropy);
            game.getGameDTO().getEntropies().add((float)  entropy);
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


    public static void storeSearchResults(Game game, @NotNull Node root, boolean justPriorValues, MuZeroConfig config, Action selectedAction, MinMaxStats minMaxStats, MinMaxStats minMaxStatsEntropyQValues) {



        game.getGameDTO().getRootValueTargets().add((float) root.getImprovedValue());

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
