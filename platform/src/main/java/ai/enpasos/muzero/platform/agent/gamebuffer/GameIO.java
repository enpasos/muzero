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

import ai.enpasos.muzero.platform.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.enpasos.muzero.platform.MuZero.getGamesBasedir;
import static ai.enpasos.muzero.platform.MuZero.getNetworksBasedir;

@Slf4j
public class GameIO {
    private static int latestGameNo = -1;

    public static List<Game> readGames(@NotNull MuZeroConfig config) {
        try (Stream<Path> walk = Files.walk(Paths.get(getGamesBasedir(config)))) {
            return walk.filter(path -> !path.toString().endsWith("games") && !path.toString().contains("buffer"))
                    // .limit(3000)
                    .map(path -> Game.decode(config, loadPathAsByteArray(path)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull Game readGame(int i, @NotNull MuZeroConfig config) {
        try {
            return Game.decode(config, Files.readAllBytes(Paths.get(getGamesBasedir(config) + "/game" + i)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] loadPathAsByteArray(@NotNull Path path) {
        try {
            log.debug("readGame " + path);
            return FileUtils.readFileToByteArray(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getLatestObjectNo(@NotNull MuZeroConfig config) {

        try (Stream<Path> walk = Files.walk(Paths.get(getNetworksBasedir(config)))) {
            OptionalInt no = walk.filter(Files::isDirectory).filter(path -> !path.toString().endsWith("networks"))
                    .mapToInt(path -> Integer.parseInt(path.toString().substring(path.toString().lastIndexOf("\\") + 1)))
                    .max();
            if (no.isPresent()) {
                return no.getAsInt();
            } else {
                return 0;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static int getNewLatestGameNo(@NotNull MuZeroConfig config) {
        getLatestGameNo(config);
        latestGameNo++;
        return latestGameNo;
    }

    public static void getLatestGameNo(@NotNull MuZeroConfig config) {
        if (latestGameNo == -1) {
            try (Stream<Path> walk = Files.walk(Paths.get(getGamesBasedir(config)))) {
                OptionalInt no = walk.filter(Files::isRegularFile)
                        .mapToInt(path -> Integer.parseInt(path.toString().substring((getGamesBasedir(config) + "/game").length())))
                        .max();
                if (no.isPresent()) {
                    latestGameNo = no.getAsInt();
                } else {
                    latestGameNo = 0;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static int getLatestBufferNo(@NotNull MuZeroConfig config) {
        Path gamesPath = Paths.get(getGamesBasedir(config));
        if (Files.notExists(gamesPath)) {
            try {
                Files.createFile(Files.createDirectories(gamesPath)).toFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (Stream<Path> walk = Files.walk(gamesPath)) {
            OptionalInt no = walk.filter(Files::isRegularFile)
                    .mapToInt(path -> Integer.parseInt(path.toString().substring((getGamesBasedir(config) + "/buffer").length())))
                    .max();
            if (no.isPresent()) {
                return no.getAsInt();
            } else {
                return 0;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
