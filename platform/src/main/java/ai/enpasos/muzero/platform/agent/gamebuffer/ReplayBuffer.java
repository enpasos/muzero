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

package ai.enpasos.muzero.platform.agent.gamebuffer;


import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.MuZero;
import ai.enpasos.muzero.platform.agent.fast.model.Sample;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static ai.enpasos.muzero.platform.agent.gamebuffer.GameIO.getLatestBufferNo;


@Data
@Slf4j
public class ReplayBuffer {

    public static final double BUFFER_IO_VERSION = 1.0;
    private int batchSize;
    private ReplayBufferDTO buffer;

    private MuZeroConfig config;

    public ReplayBuffer(@NotNull MuZeroConfig config) {
        this.config = config;
        this.batchSize = config.getBatchSize();
        this.buffer = new ReplayBufferDTO(config.getWindowSize(), config.getGameClass().getCanonicalName());
    }

    public static @NotNull Sample sampleFromGame(int numUnrollSteps, int tdSteps, @NotNull Game game, NDManager ndManager, ReplayBuffer replayBuffer, MuZeroConfig config) {
        int gamePos = samplePosition(game);
        return sampleFromGame(numUnrollSteps, tdSteps, game, gamePos, ndManager, replayBuffer, config);
    }


    public static @NotNull Sample sampleFromGame(int numUnrollSteps, int tdSteps, @NotNull Game game, int gamePos, NDManager ndManager, ReplayBuffer replayBuffer, MuZeroConfig config) {
        Sample sample = new Sample();
        game.replayToPosition(gamePos);

        sample.setObservation(game.getObservation(ndManager));

        List<Integer> actions = new ArrayList<>(game.getGameDTO().getActionHistory());
        if (actions.size() < gamePos + numUnrollSteps) {
            actions.addAll(game.getRandomActionsIndices(gamePos + numUnrollSteps - actions.size()));

        }


        sample.setActionsList(actions.subList(gamePos, gamePos + numUnrollSteps));


        sample.setTargetList(game.makeTarget(gamePos, numUnrollSteps, tdSteps, game.toPlay(), sample, config));

        return sample;
    }


    public static int samplePosition(@NotNull Game game) {
        int numActions = game.getGameDTO().getActionHistory().size();
        return ThreadLocalRandom.current().nextInt(0, numActions + 1);  // one more positions than actions
    }

    public static @NotNull ReplayBufferDTO decodeDTO(byte @NotNull [] bytes) {
        String json = new String(bytes, StandardCharsets.UTF_8);
        return getGson().fromJson(json, ReplayBufferDTO.class);
    }

    @NotNull
    private static Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setVersion(BUFFER_IO_VERSION);
        return builder.create();
    }

    public static byte @NotNull [] encodeDTO(ReplayBufferDTO dto) {
        String json = getGson().toJson(dto);
        return json.getBytes(StandardCharsets.UTF_8);
    }


    public void saveGame(@NotNull Game game) {


        buffer.saveGame(game, config);

    }

    /**
     * @param numUnrollSteps number of actions taken after the chosen position (if there are any)
     */
    public List<Sample> sampleBatch(int numUnrollSteps, int tdSteps, NDManager ndManager) {

        return sampleGames().stream()
                .map(game -> sampleFromGame(numUnrollSteps, tdSteps, game, ndManager, this, config))
                .collect(Collectors.toList());

    }


    // for "fair" training do train the strengths of PlayerA and PlayerB equally
    public List<Game> sampleGames() {

        List<Game> games = new ArrayList<>(this.buffer.getGames());
        Collections.shuffle(games);


        List<Game> gamesToTrain = games.stream()
                .filter(g -> {
                    if (g instanceof ZeroSumGame) {
                        Optional<OneOfTwoPlayer> winner = ((ZeroSumGame) g).whoWonTheGame();
                        return winner.isEmpty() || winner.get() == OneOfTwoPlayer.PlayerA;
                    } else {
                        return true;
                    }
                })
                .limit(this.batchSize / 2).collect(Collectors.toList());
        games.removeAll(gamesToTrain);  // otherwise, draw games could be selected again
        gamesToTrain.addAll(games.stream()
                .filter(g -> {
                    if (g instanceof ZeroSumGame) {
                        Optional<OneOfTwoPlayer> winner = ((ZeroSumGame) g).whoWonTheGame();
                        return winner.isEmpty() || winner.get() == OneOfTwoPlayer.PlayerB;
                    } else {
                        return true;
                    }
                })
                .limit(this.batchSize / 2)
                .collect(Collectors.toList()));

        return gamesToTrain;
    }

    public void saveState() {
        String filename =  "buffer" + buffer.getCounter();
        String pathname = MuZero.getGamesBasedir(config) + File.separator + filename + ".zip";
        log.info("saving ... " + pathname);

        byte[] input = encodeDTO(this.buffer);

        try (FileOutputStream baos = new FileOutputStream(pathname)) {
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                ZipEntry entry = new ZipEntry(filename + ".json");
                entry.setSize(input.length);
                zos.putNextEntry(entry);
                zos.write(input);
                zos.closeEntry();
            }
        } catch (Exception e) {
            throw new MuZeroException(e);
        }
    }

    public void loadLatestState() {
        int c = getLatestBufferNo(config);
        loadState(c);
    }

    public void loadState(int c) {
        String pathname = MuZero.getGamesBasedir(config) + "/buffer" + c + ".zip";
        log.info("loading ... " + pathname);

        try (FileInputStream fis = new FileInputStream(pathname)) {
            try (ZipInputStream zis = new ZipInputStream(fis)) {
                byte[] raw =  zis.readAllBytes();
                this.buffer = decodeDTO(raw);
                rebuildGames();
                this.buffer.setWindowSize(config.getWindowSize());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void rebuildGames() {
        buffer.games = new ArrayList<>();
        for (GameDTO gameDTO : buffer.getData()) {
            Game game = this.config.newGame();
            game.setGameDTO(gameDTO);
            if (!game.terminal()) {
                game.replayToPosition(game.actionHistory().getActionIndexList().size());
            }
            buffer.games.add(game);
        }
    }


}
