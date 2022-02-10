package ai.enpasos.muzero.platform.ranking;

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
import java.nio.charset.StandardCharsets;

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
}
