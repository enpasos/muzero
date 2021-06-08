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
import ai.enpasos.muzero.agent.fast.model.Sample;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

    public ReplayBuffer(@NotNull MuZeroConfig config) {
        this.config = config;
        this.batchSize = config.getBatchSize();
        this.buffer = new ReplayBufferDTO(config.getWindowSize());
    }

    public static @NotNull Sample sampleFromGame(int numUnrollSteps, int tdSteps, @NotNull Game game, NDManager ndManager) {
        int gamePos = samplePosition(game);
        return sampleFromGame(numUnrollSteps, tdSteps, game, gamePos, ndManager);
    }


    public static @NotNull Sample sampleFromGame(int numUnrollSteps, int tdSteps, @NotNull Game game, int gamePos, NDManager ndManager) {
        Sample sample = new Sample();
        game.replayToPosition(gamePos);
        sample.setObservation(game.getObservation(ndManager));


        // int historyMax = Math.min(gamePos + numUnrollSteps, game.getGameDTO().getActionHistory().size());

        List<Integer> actions = new ArrayList<>(game.getGameDTO().getActionHistory());
        if (actions.size() < gamePos + numUnrollSteps) {
            actions.addAll(game.getRandomActionsIndices(gamePos + numUnrollSteps - actions.size()));
        }


        sample.setActionsList(actions.subList(gamePos, gamePos + numUnrollSteps));


        sample.setTargetList(game.makeTarget(gamePos, numUnrollSteps, tdSteps, game.toPlay()));

        return sample;
    }


    public static int samplePosition(@NotNull Game game) {
        int numActions = game.getGameDTO().getActionHistory().size();
        return ThreadLocalRandom.current().nextInt(0, numActions + 1);  // one more positions than actions
    }

    public static @NotNull ReplayBufferDTO decodeDTO(byte @NotNull [] bytes) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);


        try (ObjectInputStream objectInputStream = new ObjectInputStream(new GZIPInputStream(byteArrayInputStream))) {
            return (ReplayBufferDTO) objectInputStream.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public static byte @NotNull [] encodeDTO(ReplayBufferDTO dto) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new GZIPOutputStream(byteArrayOutputStream))) {
            objectOutputStream.writeObject(dto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return byteArrayOutputStream.toByteArray();
    }


    public void saveGame(@NotNull Game game) {


        buffer.saveGame(game.gameDTO);

    }

    /**
     * @param numUnrollSteps number of actions taken after the chosen position (if there are any)
     */
    public List<Sample> sampleBatch(int numUnrollSteps, int tdSteps, NDManager ndManager) {
        //        long start = System.currentTimeMillis();

        return sampleGames().stream()
                .map(game -> sampleFromGame(numUnrollSteps, tdSteps, game, ndManager))
                .collect(Collectors.toList());

    }


    public List<Game> sampleGames() {

//        long start = System.currentTimeMillis();

        List<GameDTO> games = new ArrayList<>(this.buffer.getData());
        Collections.shuffle(games);


        return games.stream()
                .limit(this.batchSize)
                .map(dto -> {
                    Game game = config.newGame();
                    Objects.requireNonNull(game).setGameDTO(dto);
                    return game;
                })
                .collect(Collectors.toList());
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
            this.buffer = decodeDTO(raw);
            this.buffer.setWindowSize(config.getWindowSize());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
