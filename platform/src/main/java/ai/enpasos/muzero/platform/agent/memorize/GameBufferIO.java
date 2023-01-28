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


import ai.enpasos.muzero.platform.agent.memory.protobuf.GameBufferProto;
import ai.enpasos.muzero.platform.common.Constants;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.FileType;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static ai.enpasos.muzero.platform.agent.memorize.GameBufferDTO.BUFFER_IO_VERSION;


@Data
@Slf4j
@Component
public class GameBufferIO {


  //  public static final String NODETREE_JSON = "nodetree.json";
    @Autowired
    MuZeroConfig config;

    @NotNull
    private static Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setVersion(BUFFER_IO_VERSION);
        return builder.create();
    }

    public static byte @NotNull [] encodeDTO(Object dto) {
        try {
            String json = getGson().toJson(dto);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }

    }

    public static @NotNull GameBufferDTO decodeGameBufferDTO(byte @NotNull [] bytes) {
        String json = new String(bytes, StandardCharsets.UTF_8);
        return getGson().fromJson(json, GameBufferDTO.class);
    }

    public void saveGames(List<Game> games, String networkName, MuZeroConfig config) {

        GameBufferDTO gameBufferDTO = new GameBufferDTO(config);
        gameBufferDTO.setGames(games);

        saveState(gameBufferDTO, networkName);

    }

//    public List<Game> loadGames(String networkName, MuZeroConfig config) {
//        List<Game> games = null;
//        GameBufferDTO gameBufferDTO = new GameBufferDTO(config);
//        gameBufferDTO.setGames(games);
//        saveState(gameBufferDTO, networkName);
//        return games;
//    }

    public List<Game> loadGamesForReplay(int n , List<String> networkNamesNotToLoad, MuZeroConfig config) {
        List<Game> games = new ArrayList<>();
        GameBufferDTO gameBufferDTO = new GameBufferDTO(config);


        List<Path> paths = this.getBufferNames();
        List<Path> pathsNotToLoad = new ArrayList<>();
        for (Path path : paths) {
            for ( String networkName : networkNamesNotToLoad) {
                if (path.toString().contains(networkName)) {
                    pathsNotToLoad.add(path);
                }
            }
         }

        paths.removeAll(pathsNotToLoad);
        Collections.shuffle(paths);
        for (int h = 0; h < paths.size() && games.size() <= n; h++) {
            Path path = paths.get(paths.size() - 1 - h);
            GameBufferDTO gameBufferDTO2 = this.loadState(path);
             games.addAll(gameBufferDTO.getGames());
        }

        return games.subList(0, Math.min(n, games.size()));
    }

//private int getEpochFromPath(Path path) {
//        return Integer.parseInt(path.toString().substring((config.getGamesBasedir() + Constants.BUFFER_DIR).length()).replace("proto", "").replace(".zip", ""));
//    }
//    private int getNetworkNameFromPath(Path path) {
//        return Integer.parseInt(path.toString().substring((config.getGamesBasedir() + Constants.BUFFER_DIR).length()).replace("proto", "").replace(".zip", ""));
//    }

    public void saveState(GameBufferDTO dto, String networkName) {


        String pathname = config.getGamesBasedir() + File.separator + networkName + "-jsonbuf.zip";

        byte[] input;

        List<String> networkNames = dto.games.stream().map(g -> g.getGameDTO().getNetworkName()).distinct().collect(Collectors.toList());

        if (config.getGameBufferWritingFormat() == FileType.ZIPPED_JSON) {
            log.info("saving ... " + pathname);
            try (FileOutputStream baos = new FileOutputStream(pathname)) {
                try (BufferedOutputStream bos = new BufferedOutputStream(baos)) {

                    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                        for (String networkNameHere : networkNames) {
                            dto.games.stream()
                                .filter(g -> g.getGameDTO().getNetworkName().equals(networkNameHere))
                                .forEach(g -> dto.getInitialGameDTOList().add(g.getGameDTO()));
                            input = encodeDTO(dto);
                            dto.getInitialGameDTOList().clear();

                            ZipEntry entry = new ZipEntry(networkNameHere + ".json");
                            entry.setSize(input.length);
                            zos.putNextEntry(entry);
                            zos.write(input);
                            zos.closeEntry();
                        }

                    }
                }
            } catch (Exception e) {
                throw new MuZeroException(e);
            }

        }


        if (config.getGameBufferWritingFormat() == FileType.ZIPPED_PROTOCOL_BUFFERS) {

            pathname = config.getGamesBasedir() + File.separator + networkName + "-protobuf.zip";
            log.info("saving ... " + pathname);


            try (FileOutputStream baos = new FileOutputStream(pathname)) {
                // use Buffered Output Stream for performance
                try (BufferedOutputStream bos = new BufferedOutputStream(baos)) {
                    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                        for (String networkNameHere : networkNames) {
                            GameBufferDTO dtoHere = dto.copyEnvelope();
                            dto.games.stream()
                                .filter(g -> g.getGameDTO().getNetworkName().equals(networkNameHere))
                                .forEach(g -> dtoHere.getInitialGameDTOList().add(g.getGameDTO()));

                            GameBufferProto proto = dto.proto();
                            input = proto.toByteArray();

                            ZipEntry entry = new ZipEntry(networkNameHere + ".dat");
                            entry.setSize(input.length);
                            zos.putNextEntry(entry);
                            zos.write(input);
                            zos.closeEntry();

                        }

                    }
                }
            } catch (Exception e) {
                throw new MuZeroException(e);
            }
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

    public GameBufferDTO loadState(Path path) {

        String pathname = path.toString();
        log.info("loading ... " + pathname);
        return loadFromProtobuf(pathname, Optional.of(this::loadStateFromJson));

    }


    @Nullable
    private GameBufferDTO loadFromProtobuf(String pathname, Optional<Function<String, GameBufferDTO>> optionalOtherLoader) {
        GameBufferDTO dto = null;


        try (FileInputStream fis = new FileInputStream(pathname)) {
            try (BufferedInputStream bis = new BufferedInputStream(fis)) {
                try (ZipInputStream zis = new ZipInputStream(fis)) {
                    ZipEntry entry;

                    while ((entry = zis.getNextEntry()) != null) {
                        String filename = entry.getName();
                        if (filename.endsWith(".dat")) {
                            log.info("load: " + filename);
                            byte[] bytes = zis.readAllBytes();

                            GameBufferProto proto = GameBufferProto.parseFrom(bytes);
                            GameBufferDTO dtoHere = GameBufferDTO.deproto(proto, config);
                            if (dto == null) {
                                dto = dtoHere;
                            } else {
                                dto.getInitialGameDTOList().addAll(dtoHere.getInitialGameDTOList());
                            }

                        }
                    }
                    Objects.requireNonNull(dto).rebuildGames(config, false);
                }
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
            if (optionalOtherLoader.isPresent())
                dto = optionalOtherLoader.get().apply(pathname);
        }
        return dto;
    }

    private GameBufferDTO loadStateFromJson(String pathname) {
        return loadStateFromJson(pathname, Optional.empty());
    }

    @Nullable
    // add a function as the second parameter
    private GameBufferDTO loadStateFromJson(String pathname, Optional<Function<String, GameBufferDTO>> optionalOtherLoader) {
        GameBufferDTO dto = null;
        try (FileInputStream fis = new FileInputStream(pathname)) {
            try (ZipInputStream zis = new ZipInputStream(fis)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String filename = entry.getName();
                    if (filename.endsWith(".dat")) {
                        log.info("load: " + filename);
                        byte[] raw = zis.readAllBytes();
                        GameBufferDTO dtoHere = decodeGameBufferDTO(raw);
                        if (dto == null) {
                            dto = dtoHere;
                        } else {
                            dto.getInitialGameDTOList().addAll(dtoHere.getInitialGameDTOList());
                        }
                    }
                }
                Objects.requireNonNull(dto).rebuildGames(config, false);
            }
        } catch (Exception e2) {
            log.warn(e2.getMessage());
            if (optionalOtherLoader.isPresent())
                dto = optionalOtherLoader.get().apply(pathname);
        }
        return dto;
    }


}
