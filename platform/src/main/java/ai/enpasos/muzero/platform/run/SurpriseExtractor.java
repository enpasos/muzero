/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ai.enpasos.muzero.platform.run;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.platform.agent.intuitive.Inference;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Map.entry;


@Slf4j
@Component
public class SurpriseExtractor {

    @Autowired
    MuZeroConfig config;

    @Autowired
    ReplayBuffer replayBuffer;

    @Autowired
    Inference inference;
    @Autowired
    Surprise surprise;

    public static float arrayMax(float[] arr) {
        float max = Float.NEGATIVE_INFINITY;

        for (float cur : arr)
            max = Math.max(max, cur);
        return max;
    }

    @SuppressWarnings("squid:S1141")
    public String listValuesForTrainedNetworks(Game game) {

        List<Float> values = game.getGameDTO().getRootValuesFromInitialInference();
        List<Integer> actions = game.getGameDTO().getActions();
        StringWriter stringWriter = new StringWriter();

        List<Float> surprises = game.getGameDTO().getSurprises();

       System.out.println("epoch;" + game.getEpoch());

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.builder().setDelimiter(';').setHeader("t", "vPlayerA", "surprise", "action").build())) {
            for (int t = 0; t < values.size(); t++) {

                float value = values.get(t);
                float surpriseLocal = surprises.get(t);
                try {
                    double valuePlayer = value;
                    if (config.getPlayerMode() == PlayerMode.TWO_PLAYERS) {
                        valuePlayer *= Math.pow(-1, t);
                    }
                    csvPrinter.printRecord(t,
                        NumberFormat.getNumberInstance().format(valuePlayer),
                        NumberFormat.getNumberInstance().format(surpriseLocal),
                        t<actions.size() ? actions.get(t) : "");
                } catch (Exception e) {
                    // ignore
                }

            }

        } catch (IOException e) {
            throw new MuZeroException(e);
        }

        return stringWriter.toString();
    }

    @NotNull
    public Optional<Game> getGame() {
        replayBuffer.loadLatestState();
        return Optional.of(replayBuffer.getBuffer().getGames().get(replayBuffer.getBuffer().getGames().size() - 1));

    }

    @NotNull
    public Optional<Game> getGame(int no) {
        replayBuffer.loadLatestState();
        return Optional.of(replayBuffer.getBuffer().getGames().get(no));
    }

    @NotNull
    public Optional<Game> getGameWithHighestSurprise() {

        replayBuffer.loadLatestState();

        // return the game with the highest surprise
        replayBuffer.getBuffer().getGames().forEach(game -> {
                float max = Float.MIN_VALUE;
                for (float v : game.getGameDTO().getSurprises()) {
                    if (v > max) {
                        max = v;
                    }
                }
                game.setSurpriseMax(max);
            }
        );
        return replayBuffer.getBuffer().getGames().stream().max((g1, g2) -> (int) (g1.getSurpriseMax() - g2.getSurpriseMax()));

    }

    public Optional<Game> getGameStartingWithActions(int... actions) {
        List<Integer> actionsList = Arrays.stream(actions).boxed().collect(Collectors.toList());
        return getGameStartingWithActions(actionsList);
    }

    public Optional<Game> getGameStartingWithActions(List<Integer> actionsList) {
        replayBuffer.loadLatestState();
        List<Game> games = replayBuffer.getBuffer().getGames();
        return games.stream().filter(game ->
            // check if game.getGameDTO().getActions() starts with actionsList
            game.getGameDTO().getActions().stream().limit(actionsList.size()).collect(Collectors.toList()).equals(actionsList)
        ).findFirst();
    }

    public Optional<Game> getGameStartingWithActionsFromStart(int... actions) {
        return getGameStartingWithActionsFromStartForEpoch(  -1, actions);
    }

    public Optional<Game> getGameStartingWithActionsFromStartForEpoch(int epoch,  int... actions) {

        Game game = this.config.newGame();
        game.apply(actions);
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            Network network = null;
            if (epoch == -1) {
                network = new Network(config, model);
            } else {
                network = new Network(config, model, Paths.get(config.getNetworkBaseDir()), Map.ofEntries(entry("epoch", epoch + "")));
            }
            game.setEpoch(NetworkHelper.getEpochFromModel(network.getModel()));
            surprise.measureValueAndSurprise(network, List.of(game));
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.of(game);

    }
}
