package ai.enpasos.muzero.go.ranking;

import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.enpasos.muzero.go.ranking.Elo.calculateNewElo;

@Component
@Slf4j
@Data
public class Ranking {


    public static final double RANKING_IO_VERSION = 1.0;
    @Autowired
    MuZeroConfig config;
    RankingListDTO rankingList = new RankingListDTO();

    public static @NotNull RankingListDTO decodeDTO(byte @NotNull [] bytes) {
        String json = new String(bytes, StandardCharsets.UTF_8);
        return getGson().fromJson(json, RankingListDTO.class);
    }

    @NotNull
    private static Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.setVersion(RANKING_IO_VERSION);
        return builder.create();
    }

    public static byte @NotNull [] encodeDTO(RankingListDTO dto) {
        String json = getGson().toJson(dto);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    public void saveRanking() {
        String pathname = getPathname();
        byte[] input;
        log.info("saving ... " + pathname);
        input = encodeDTO(this.rankingList);
        try (FileOutputStream baos = new FileOutputStream(pathname)) {
            baos.write(input);
        } catch (Exception e) {
            throw new MuZeroException(e);
        }
    }

    @NotNull
    private String getPathname() {
        String pathname;
        pathname = config.getNetworkBaseDir() + File.separator + "ranking.json";
        return pathname;
    }

    public void loadRanking() {
        String pathname = getPathname();
        try (FileInputStream fis = new FileInputStream(pathname)) {
            log.info("loading ... " + pathname);
            byte[] raw = fis.readAllBytes();
            this.rankingList = decodeDTO(raw);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e.getMessage(), e);
        }

    }

    public void assureAllPlayerInRankingList() {

        Path networkPath = Paths.get(config.getNetworkBaseDir());
        if (Files.notExists(networkPath)) {
            return;
        }
        List<String> players = null;
        try (Stream<Path> walk = Files.walk(networkPath)) {
            players = walk.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".params"))
                .mapToInt(path -> {
                    String pathStr = path.toString();
                    String noStr = pathStr.substring(pathStr.length() - "0000.params".length(), pathStr.length() - "params".length() - 1);
                    return Integer.parseInt(noStr);
                })
                .mapToObj(i -> i + "")
                .collect(Collectors.toList());

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }

        List<String> playerInRanking = this.rankingList.getRankings().stream().map(r -> r.epochPlayer + "").collect(Collectors.toList());
        players.removeAll(playerInRanking);

        addPlayersToRanking(players);

    }

    private void addPlayersToRanking(List<String> players) {
        players.stream().forEach(p ->
            this.rankingList.rankings.add(RankingEntryDTO.builder()
                .epochPlayer(Integer.parseInt(p))
                .build())
        );
    }

    public void clear() {
        this.rankingList.rankings.clear();
    }


    public int getElo(int playerEpoch) {
        RankingEntryDTO dto = getRankingEntryDTO(playerEpoch);
        return dto.getElo();
    }

    public void setElo(int playerEpoch, int elo) {
        RankingEntryDTO dto = getRankingEntryDTO(playerEpoch);
        dto.setElo(elo);
    }

    private RankingEntryDTO getRankingEntryDTO(int playerEpoch) {
        return this.rankingList.rankings.stream().filter(r -> r.epochPlayer == playerEpoch).findFirst().orElseThrow();
    }

    public void addBattle(int a, int b, double resultPlayerA, int numGamesPerBattle) {

        RankingEntryDTO rA = getRankingEntryDTO(a);
        RankingEntryDTO rB = getRankingEntryDTO(b);

        int newEloA = calculateNewElo(rA.elo, rB.elo, resultPlayerA);
        int newEloB = calculateNewElo(rB.elo, rA.elo, 1d - resultPlayerA);

        rA.battles.add(
            BattleDTO.builder()
                .epochPlayer(a)
                .epochOpponent(b)
                .numGamesPlayed(numGamesPerBattle)
                .result(resultPlayerA)
                .eloBefore(rA.elo)
                .eloAfter(newEloA)
                .build()
        );
        rB.battles.add(
            BattleDTO.builder()
                .epochPlayer(b)
                .epochOpponent(a)
                .numGamesPlayed(numGamesPerBattle)
                .result(1d - resultPlayerA)
                .eloBefore(rB.elo)
                .eloAfter(newEloB)
                .build()
        );

        rA.setElo(newEloA);
        rB.setElo(newEloB);
    }

    public boolean exists() {
        File f = new File(getPathname());
        return f.exists();
    }

    public void fillMissingRankingsByLinearInterpolation() {
        int high = selectPlayerWithHighestEpoch();
        int low = selectPlayerWithLowestEpoch();
        int next = low;
        do {
            next = findNextPlayerWithElo(low, high);
            interpolateElo(low, next);
            low = next;
        } while (next != high);
    }

    private void interpolateElo(int low, int high) {
        var eloLow = getElo(low);
        var eloHigh = getElo(high);
        double m = (double) (eloHigh - eloLow) / (double) (high - low);
        for (int x = low + 1; x < high; x++) {
            setElo(x, (int) Math.round(eloLow + m * (x - low)));
        }
    }

    private int findNextPlayerWithElo(int low, int high) {
        if (
            getElo(low) == Integer.MIN_VALUE
                || getElo(high) == Integer.MIN_VALUE
        ) throw new MuZeroException("players " + low + " and " + high + " are expected to have an elo.");
        for (int p = low + 1; p <= high; p++) {
            if (getElo(p) != Integer.MIN_VALUE) return p;
        }
        return high;
    }

    public int selectPlayerWithHighestEpochThatHasRanking() {
        return this.rankingList.rankings.stream().filter(r -> r.elo != Integer.MIN_VALUE).mapToInt(r -> r.epochPlayer).max().getAsInt();
    }

    public int selectPlayerWithHighestEpoch() {
        return this.rankingList.rankings.stream().mapToInt(r -> r.epochPlayer).max().getAsInt();
    }

    public int selectPlayerWithLowestEpoch() {
        return this.rankingList.rankings.stream().mapToInt(r -> r.epochPlayer).min().getAsInt();
    }
}
