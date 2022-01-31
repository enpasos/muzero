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

package ai.enpasos.muzero.platform.debug;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.util.Pair;
import ai.enpasos.muzero.platform.agent.Inference;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.gamebuffer.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.slow.play.SelfPlay;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class ValueSelfconsistency {
    @Autowired
    MuZeroConfig config;

    @Autowired
    ValueExtractor valueExtractor;

    @Autowired
    ReplayBuffer replayBuffer;


    @Autowired
    SelfPlay selfPlay;


    @Autowired
    Inference inference;


    @SuppressWarnings("squid:S3740")
    public void run(boolean debug) {

//        config.setNetworkBaseDir(config.getOutputDir() + "/networks");
//
//        replayBuffer.loadLatestState();


        List<Game> games = replayBuffer.getBuffer().getGames();

        games.stream().forEach(g -> g.beforeReplay());

        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            Network network = new Network(config, model);
            games = selfPlay.justReplayGamesWithInitialInference(network, replayBuffer.getBuffer().getGames());
        }

        this.replayBuffer.getBuffer().setGames(games);
        this.replayBuffer.getBuffer().setData(
            games.stream().map(g -> g.getGameDTO()).collect(Collectors.toList())
        );

        for (int g = 0; g < games.size(); g++) {
            float error = games.get(g).calculateSquaredDistanceBetweenOriginalAndCurrentValue();
            replayBuffer.getBuffer().getData().get(g).setLastValueError(error);
        }
        //games.stream().forEach(g -> g.calculateSquaredDistanceBetweenOriginalAndCurrentValue());

        // TODO debug output

        if (debug) {
            Game game = games.get(games.size()-1);
            List<Pair> pairs = new ArrayList<>();
            for(int i = 0; i < game.getGameDTO().getRootValuesFromInitialInference().size(); i++) {
                float v1 = game.getOriginalGameDTO().getRootValuesFromInitialInference().get(i);
                float v2 = game.getGameDTO().getRootValuesFromInitialInference().get(i);
                System.out.println(i + "; " + NumberFormat.getNumberInstance().format(v1)+ "; " + NumberFormat.getNumberInstance().format(v2)+ "; ");
            }
        }

        this.replayBuffer.sortGamesByLastValueError();

        this.replayBuffer.removeHighLastValueErrorGames();

        games.stream().forEach(g -> g.afterReplay());
        this.replayBuffer.getBuffer().setData(
            games.stream().map(g -> g.getGameDTO()).collect(Collectors.toList())
        );


      //  replayBuffer.rebuildGames();




//        IntStream.rangeClosed(1, config.getNumParallelGamesPlayed())
//            .mapToObj(i -> config.newGame())
//            .collect(Collectors.toList());



//        for (int i = 0; i < replayBuffer.getBuffer().getGames().size() ; i = i + config.getNumParallelGamesPlayed()) {
//            Game game = replayBuffer.getBuffer().getGames().get(i);
//
//            List<Game> games = replayBuffer.getBuffer().getGames().subList(i, i+config.getNumParallelGamesPlayed());
//
//            List<List<Pair<Double, Double>>> values = getValuesOriginalAndNew(game);
//
//            double[] errors = values.stream().mapToDouble(p -> {
//                double d = p.getKey() - p.getValue();
//                return d * d;
//            }).toArray();
//
//            //     Arrays.stream(errors).forEach(e -> System.out.println(NumberFormat.getNumberInstance().format(e)));
//
//            double error = Arrays.stream(errors).sum() / values.size();
//            System.out.println(i + "; " + error);
//
//           // printValues(values);
//        }


        //  System.out.println(originalValues);

//        replayBuffer.loadLatestState();
//
//        List<Pair> pairs = replayBuffer.getBuffer().getGames().stream().map(g -> new Pair(g.actionHistory().getActionIndexList(), g.getLastReward()))
//                .sorted(Comparator.comparing((Pair p) -> ((Float) p.getValue())).thenComparing(p -> p.getKey().toString()))
//                .collect(Collectors.toList());
//
//        pairs.forEach(p -> System.out.println(p.getKey() + "; " + p.getValue()));

    }

    private void printValues(List<Pair<Double, Double>> values) {
        StringWriter stringWriter = new StringWriter();
        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL.withDelimiter(';').withHeader("t", "originalValue", "newValue"))) {
            IntStream.range(0, values.size() + 1).forEach(
                t -> {
                    try {
                        csvPrinter.printRecord(t,
                            NumberFormat.getNumberInstance().format(values.get(t).getKey()),
                            NumberFormat.getNumberInstance().format(values.get(t).getValue()));

                    } catch (Exception e) {
                        // ignore
                    }
                });

        } catch (IOException e) {
            throw new MuZeroException(e);
        }

        System.out.println(stringWriter.toString());
    }
//    public List<List<Pair<Double, Double>>> getValuesOriginalAndNew(List<Game> games) {
//
//      //  List<Integer> actions = game.actionHistory().getActionIndexList();
//        // log.debug(actions.toString());
//
//        int maxActionNumber = games.stream().mapToInt(g -> g.actionHistory().getActionIndexList().size()).max().getAsInt();
//
//        List<List<Float>> originalValues = games.stream().map(g -> g.getGameDTO().getRootValuesFromInitialInference()).collect(Collectors.toList());
//
//
//        List<Pair<Double, Double>> values = new ArrayList<>();
//
//        Game game = null;
//
//        IntStream.range(0, maxActionNumber + 1).forEach(
//            t -> {
//                try {
//                    double[] valueOriginal = originalValues.stream().mapToDouble(vs -> {
//                        if (t < vs.size()) {
//                            return vs.get(t);
//                        } else {
//                            return Double.NaN;
//                        }
//                    }).toArray();
//
//                    game
//
//
//
//
//                    double[] valueNew = inference.aiValue(games);
//                    if (config.getPlayerMode() == PlayerMode.TWO_PLAYERS) {
//                        double sign = Math.pow(-1, t);
//                        valueNew *= sign;
//                        valueOriginal *= sign;
//                        values.add(new Pair<>(valueOriginal, valueNew));
//                    }
//                } catch (Exception e) {
//                    // ignore
//                }
//            });
//
//        return values;
//    }

    public List<Pair<Double, Double>> getValuesOriginalAndNew(Game game) {

        List<Integer> actions = game.actionHistory().getActionIndexList();
        // log.debug(actions.toString());
        List<Float> originalValues = game.getGameDTO().getRootValuesFromInitialInference();

        List<Pair<Double, Double>> values = new ArrayList<>();

        IntStream.range(0, actions.size() + 1).forEach(
            t -> {
                try {
                    double valueOriginal = originalValues.get(t);
                    double valueNew = inference.aiValue(actions.subList(0, t), config.getNetworkBaseDir());
                    if (config.getPlayerMode() == PlayerMode.TWO_PLAYERS) {
                        double sign = Math.pow(-1, t);
                        valueNew *= sign;
                        valueOriginal *= sign;
                        values.add(new Pair<>(valueOriginal, valueNew));
                    }
                } catch (Exception e) {
                    // ignore
                }
            });

        return values;
    }

}
