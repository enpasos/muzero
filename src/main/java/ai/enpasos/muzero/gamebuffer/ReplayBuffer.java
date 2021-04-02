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

package ai.enpasos.muzero.gamebuffer;


import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.MuZero;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.network.Sample;
import lombok.Data;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static ai.enpasos.muzero.gamebuffer.GameIO.getLatestBufferNo;


@Data
public class ReplayBuffer {
    private int batchSize;
    private ReplayBufferDTO buffer;
    private MuZeroConfig config;

    public ReplayBuffer(MuZeroConfig config) {
        this.config = config;
        this.batchSize = config.getBatchSize();
        this.buffer = new ReplayBufferDTO(config.getWindowSize());
    }

    public static Sample sampleFromGame(int numUnrollSteps, int tdSteps, Game game, NDManager ndManager) {
        int gamePos = samplePosition(game);
        return sampleFromGame(numUnrollSteps, tdSteps, game, gamePos, ndManager);
    }


    public static Sample sampleFromGame(int numUnrollSteps, int tdSteps, Game game, int gamePos, NDManager ndManager) {
        Sample sample = new Sample();
        game.replayToPosition(gamePos);
        sample.setObservation(game.getObservation(ndManager));


        // int historyMax = Math.min(gamePos + numUnrollSteps, game.getGameDTO().getActionHistory().size());

        List<Integer> actions = new ArrayList<>();
        actions.addAll(game.getGameDTO().getActionHistory());
        if (actions.size() < gamePos + numUnrollSteps) {
            actions.addAll(game.getRandomActionsIndices(gamePos + numUnrollSteps - actions.size()));
        }


        sample.setActionsList(actions.subList(gamePos, gamePos + numUnrollSteps));


        sample.setTargetList(game.makeTarget(gamePos, numUnrollSteps, tdSteps, game.toPlay()));

//        double value = sample.targetList.get(sample.targetList.size()-1).value;
//        System.out.println("target value: " + value);

        return sample;
    }


    public static int samplePosition(Game game) {
        int numActions = game.getGameDTO().getActionHistory().size();
        return ThreadLocalRandom.current().nextInt(0, numActions + 1);  // one more positions than actions
    }

    public static ReplayBufferDTO decodeDTO(MuZeroConfig config, byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);


        try (ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(bais))) {
            return (ReplayBufferDTO) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public static byte[] encodeDTO(ReplayBufferDTO dto) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(baos))) {
            oos.writeObject(dto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return baos.toByteArray();
    }


    public void saveGame(Game game) {


        buffer.saveGame(game.gameDTO);

    }

    /**
     * @param numUnrollSteps number of actions taken after the chosen position (if there are any)
     * @param tdSteps
     */
    public List<Sample> sampleBatch(int numUnrollSteps, int tdSteps, NDManager ndManager) {
        /**
         * g.make_image(i),      the observation at the chosen position
         * g.history[i:i + num_unroll_steps],    a list of the next num_unroll_steps actions taken after the chosen position (if they exist)
         * g.make_target(i, num_unroll_steps, td_steps, g.to_play())    a list of the targets that will be used to train the neural networks. Specifically, this is a list of tuples:target_value, target_reward and target_policy.
         */
//        long start = System.currentTimeMillis();
        List<Sample> sampleBatch = sampleGames().stream()
                .map(game -> sampleFromGame(numUnrollSteps, tdSteps, game, ndManager))
                .collect(Collectors.toList());

//            long duration = (System.currentTimeMillis() - start);
//         System.out.println("duration sampleBatch [ms]: " + duration);
        return sampleBatch;

    }


    public List<Game> sampleGames() {

//        long start = System.currentTimeMillis();

        List<GameDTO> games = new ArrayList<>(this.buffer.getData().values());
        Collections.shuffle(games);


        List<Game> gamesSampleList = games.stream()
                .limit(this.batchSize)
                .map(dto -> {
                    Game game = config.newGame();
                    game.setGameDTO(dto);
                    return game;
                })
                .collect(Collectors.toList());

//        long duration = (System.currentTimeMillis() - start);
//        System.out.println("duration sampleGames [ms]: " + duration);

        return gamesSampleList;
    }

    public void saveState() {
        String pathname = MuZero.getGamesBasedir(config) + "/buffer" + buffer.getCounter();
        System.out.println("saving ... " + pathname);


        try {
            FileUtils.writeByteArrayToFile(new File(pathname), encodeDTO(this.buffer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadLatestState() {
        int c = getLatestBufferNo(config);
        String pathname = MuZero.getGamesBasedir(config) + "/buffer" + c;
        System.out.println("loading ... " + pathname);
        try {
            byte[] raw = FileUtils.readFileToByteArray(new File(pathname));
            this.buffer = decodeDTO(this.config, raw);
            this.buffer.setWindowSize(config.getWindowSize());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
