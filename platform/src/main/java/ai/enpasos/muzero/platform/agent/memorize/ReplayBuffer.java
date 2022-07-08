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

package ai.enpasos.muzero.platform.agent.memorize;


import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.djl.util.Utils;
import ai.enpasos.muzero.platform.agent.intuitive.Sample;
import ai.enpasos.muzero.platform.agent.memory.protobuf.ReplayBufferProto;
import ai.enpasos.muzero.platform.common.Constants;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.FileType;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


@Data
@Slf4j
@Component
public class ReplayBuffer {

    public static final double BUFFER_IO_VERSION = 1.0;
    private int batchSize;
    private ReplayBufferDTO buffer;


    private String currentNetworkName = "NONE";
    @Autowired
    private MuZeroConfig config;

    public static @NotNull Sample sampleFromGame(int numUnrollSteps, int tdSteps, @NotNull Game game, NDManager ndManager, ReplayBuffer replayBuffer) {
        int gamePos = samplePosition(game);
        return sampleFromGame(numUnrollSteps, tdSteps, game, gamePos, ndManager, replayBuffer);
    }

    public static @NotNull Sample sampleFromGame(int numUnrollSteps, int tdSteps, @NotNull Game game, int gamePos, NDManager ndManager, ReplayBuffer replayBuffer) {
        Sample sample = new Sample();
        game.replayToPosition(gamePos);

        sample.setObservation(game.getObservation(ndManager));

        List<Integer> actions = new ArrayList<>(game.getGameDTO().getActions());
        if (actions.size() < gamePos + numUnrollSteps) {
            actions.addAll(game.getRandomActionsIndices(gamePos + numUnrollSteps - actions.size()));

        }

        sample.setActionsList(actions.subList(gamePos, gamePos + numUnrollSteps));

        sample.setTargetList(game.makeTarget(gamePos, numUnrollSteps, tdSteps));

        return sample;
    }

    public static int samplePosition(@NotNull Game game) {
        GameDTO dto = game.getGameDTO();
        int numActions = dto.getActions().size();
        long delta = dto.getTStateB() - dto.getTStateA();
        if (delta < 0) delta = 0L;
        int enhanceFactor = 1;
        long numNormalActions = numActions - (dto.getTStateA() + delta);
        int n = (int) (enhanceFactor * (delta) + numNormalActions);

        int rawpos = ThreadLocalRandom.current().nextInt(0, n);
        int gamePos = 0;
        if (rawpos < numNormalActions) {
            gamePos = (int) (rawpos + dto.getTStateA() + delta);
        } else {
            rawpos -= numNormalActions;
            rawpos /= enhanceFactor;
            gamePos = rawpos;
        }
        if (dto.getTStateA() > 0 && gamePos < dto.getTStateA()) {
            throw new MuZeroException("gamePos < dto.getTStateA()");
        }
        return gamePos;
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
        try {
            String json = getGson().toJson(dto);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }

    }

    public void createNetworkNameFromModel(Model model, String modelName, String outputDir) {
        String epochValue = model.getProperty("Epoch");
        Path modelPath = Paths.get(outputDir);
        int epoch = 0;
        try {
            epoch = epochValue == null ? Utils.getCurrentEpoch(modelPath, modelName) + 1 : Integer.parseInt(epochValue);
        } catch (IOException e) {
            throw new MuZeroException(e);
        }
        this.currentNetworkName = String.format(Locale.ROOT, "%s-%04d", modelName, epoch);
    }

    @PostConstruct
    public void postConstruct() {
        init();
    }

    public void init() {
        this.batchSize = config.getBatchSize();
        if (this.buffer != null) {
            //this.buffer.getdata().clear();
            this.buffer.games.stream().forEach(g -> {
                g.setGameDTO(null);
                g.setOriginalGameDTO(null);
            });
            this.buffer.games.clear();
        }
        this.buffer = new ReplayBufferDTO(config.getWindowSize(), config.getGameClassName());
    }

    public void saveGame(@NotNull Game game) {
        game.getGameDTO().setNetworkName(this.currentNetworkName);
        buffer.saveGame(game, config);

    }

    /**
     * @param numUnrollSteps number of actions taken after the chosen position (if there are any)
     */
    public List<Sample> sampleBatch(int numUnrollSteps, int tdSteps, NDManager ndManager) {

        return sampleGames().stream()
            .filter(game -> game.getGameDTO().getTStateA() < game.getGameDTO().getActions().size())
            .map(game -> sampleFromGame(numUnrollSteps, tdSteps, game, ndManager, this))
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
                    return winner.isEmpty() || winner.get() == OneOfTwoPlayer.PLAYER_A;
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
                    return winner.isEmpty() || winner.get() == OneOfTwoPlayer.PLAYER_B;
                } else {
                    return true;
                }
            })
            .limit(this.batchSize / 2)
            .collect(Collectors.toList()));

        return gamesToTrain;
    }


    public void saveState() {

        ReplayBufferDTO dto = this.buffer;


        String filename = this.currentNetworkName;
        String pathname = config.getGamesBasedir() + File.separator + filename + "_jsonbuf.zip";

        byte[] input;

        if (config.getGameBufferWritingFormat() == FileType.ZIPPED_JSON) {
            log.info("saving ... " + pathname);
            input = encodeDTO(dto);
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


        if (config.getGameBufferWritingFormat() == FileType.ZIPPED_PROTOCOL_BUFFERS) {
            ReplayBufferProto proto = dto.proto();
            pathname = config.getGamesBasedir() + File.separator + filename + "_protobuf.zip";
            log.info("saving ... " + pathname);
            input = proto.toByteArray();

            try (FileOutputStream baos = new FileOutputStream(pathname)) {
                try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                    ZipEntry entry = new ZipEntry(filename + ".dat");
                    entry.setSize(input.length);
                    zos.putNextEntry(entry);
                    zos.write(input);
                    zos.closeEntry();
                }
            } catch (Exception e) {
                throw new MuZeroException(e);
            }
        }

    }

    public void loadLatestState() {
        List<Path> paths = getBufferNames();
        if (paths.size() > 0) {
            Path path = paths.get(paths.size() - 1);
            loadState(path);
        }
    }




    public List<Path> getBufferNames() {
        List<Path> paths = new ArrayList<>();
        Path gamesPath = Paths.get(config.getGamesBasedir());
        if (Files.notExists(gamesPath)) {
            try {
                Files.createFile(Files.createDirectories(gamesPath));
            } catch (IOException e) {
                log.warn(e.getMessage());
            }
        }
        try (Stream<Path> walk = Files.walk(gamesPath)) {
            paths = walk.filter(Files::isRegularFile)
                .collect(Collectors.toList());

        } catch (IOException e) {
            throw new MuZeroException(e);
        }
        Collections.sort(paths);
        return paths;
    }

    public int getLatestBufferNo() {
        Path gamesPath = Paths.get(config.getGamesBasedir());
        if (Files.notExists(gamesPath)) {
            try {
                Files.createFile(Files.createDirectories(gamesPath));
            } catch (IOException e) {
                log.warn(e.getMessage());
            }
        }
        try (Stream<Path> walk = Files.walk(gamesPath)) {
            OptionalInt no = walk.filter(Files::isRegularFile)
                .mapToInt(path -> Integer.parseInt(path.toString().substring((config.getGamesBasedir() + Constants.BUFFER_DIR).length()).replace("proto", "").replace(".zip", "")))
                .max();
            if (no.isPresent()) {
                return no.getAsInt();
            } else {
                return 0;
            }
        } catch (IOException e) {
            throw new MuZeroException(e);
        }

    }

    public void loadState(Path path) {
        init();

        String pathname = path.toString();
        log.info("loading ... " + pathname);
        try (FileInputStream fis = new FileInputStream(pathname)) {
            try (ZipInputStream zis = new ZipInputStream(fis)) {
                zis.getNextEntry();
                byte[] raw = zis.readAllBytes();
                this.buffer = decodeDTO(raw);
                rebuildGames();
                this.buffer.setWindowSize(config.getWindowSize());
            }
        } catch (Exception e) {
            try (FileInputStream fis = new FileInputStream(pathname)) {
                try (ZipInputStream zis = new ZipInputStream(fis)) {
                    zis.getNextEntry();
                    ReplayBufferProto proto = ReplayBufferProto.parseFrom(zis);
                    this.buffer.deproto(proto);
                    rebuildGames();
                    this.buffer.setWindowSize(config.getWindowSize());
                }
            } catch (Exception e2) {
                log.warn(e2.getMessage());
            }
        }


    }

    public void rebuildGames() {
        this.buffer.rebuildGames(this.config);
    }


    public void keepOnlyTheLatestGames(int n) {
        buffer.games = buffer.games.subList(Math.max(buffer.games.size() - n, 0), buffer.games.size());
        //  buffer.setdata(buffer.getdata().subList(Math.max(buffer.getdata().size() - n, 0), buffer.getdata().size()));
    }

    public void sortGamesByLastValueError() {
        this.getBuffer().getGames().sort(
            (Game g1, Game g2) -> Float.compare(g2.getError(), g1.getError()));
    }

    public void removeHighLastValueErrorGames() {
        int max = this.getConfig().getWindowValueSelfconsistencySize();
        int size = this.getBuffer().getGames().size();
        if (size <= max) return;
        this.getBuffer().setGames(this.getBuffer().getGames().subList(size - max, size));
        // this.getBuffer().setdata(this.getBuffer().getdata().subList(size - max, size));
    }

    public void removeGames(List<Game> games) {
        games.forEach(this::removeGame);
    }

    public void removeGame(Game game) {
        this.buffer.removeGame(game);
    }
}
