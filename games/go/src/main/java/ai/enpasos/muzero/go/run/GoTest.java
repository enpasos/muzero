package ai.enpasos.muzero.go.run;


import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.intuitive.Inference;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.SurpriseExtractor;
import ai.enpasos.muzero.platform.run.train.MuZero;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Component
public class GoTest {

    @Autowired
    GoStartValueExtractor goStartValueExtractor;
    @Autowired
    ReplayBuffer replayBuffer;
    @Autowired
    private MuZeroConfig config;
    @Autowired
    private MuZero muZero;

    @Autowired
    SurpriseExtractor surpriseExtractor;

    @Autowired
    Inference inference;


    @SuppressWarnings("squid:S125")
    public void run() {


        Game  game = surpriseExtractor.getGameStartingWithActionsFromStart(
            12, 17, 11, 13, 7, 23, 6, 19, 8, 9, 16, 21, 3, 20, 4, 14, 15, 22, 25, 0, 5, 1, 2, 1, 0, 25, 10
        ).orElseThrow(MuZeroException::new);

//        List<Integer> actions = List.of(12, 17, 11, 13, 7, 23, 6, 19, 8, 9, 16, 21, 3, 20, 4, 14, 15, 22, 25, 0, 5, 1, 2, 1, 0, 25, 10);
//
//        inference.aiDecision(actions, false, "./pretrained",  DeviceType.GPU);

        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {

            Network network = new Network(config, model);
            try (NDManager nDManager = network.getNDManager().newSubManager()) {

                network.initActionSpaceOnDevice(nDManager);
                network.setHiddenStateNDManager(nDManager);

                List<NetworkIO> networkOutputList = network.initialInferenceListDirect(List.of(game));
                List<Pair<Action, Double>> distributionInput = null;

                distributionInput = getPolicyOverLegalActions(game, Objects.requireNonNull(networkOutputList));



                List<NetworkIO> networkOutputList2 = network.recurrentInferenceListDirect(List.of(networkOutputList.get(0).getHiddenState()), List.of(distributionInput.get(0).getKey().encode(nDManager)));
                 getPolicyOverLegalActions(game, Objects.requireNonNull(networkOutputList2));



            }

        }
    }

    private static List<Pair<Action, Double>> getPolicyOverLegalActions(Game game, List<NetworkIO> networkOutputList) {
        List<Pair<Action, Double>> distributionInput;
        List<Action> legalActions = game.legalActions();
        float[] policyValues = networkOutputList.get(0).getPolicyValues();
        distributionInput =
          IntStream.range(0, game.getConfig().getActionSpaceSize())
              .filter(i -> {
                  Action action = game.getConfig().newAction(i);
                  return legalActions.contains(action);
              })
              .mapToObj(i -> {
                  Action action = game.getConfig().newAction(i);
                  double v = policyValues[i];
                  return new Pair<>(action, v);
              }).collect(Collectors.toList());
        return distributionInput;
    }


}
