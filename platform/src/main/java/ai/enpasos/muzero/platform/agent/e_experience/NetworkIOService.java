package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.OptionalInt;
import java.util.stream.Stream;

@Slf4j
@Component
public class NetworkIOService {

    @Autowired
    MuZeroConfig config;
    public int getLatestNetworkEpoch() {
        Path path = Paths.get(config.getNetworkBaseDir());
        if (Files.notExists(path)) {
            try {
                Files.createFile(Files.createDirectories(path));
            } catch (IOException e) {
                log.warn(e.getMessage());
            }
        }
        try (Stream<Path> walk = Files.walk(path)) {
            OptionalInt no = walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".params"))
                    .mapToInt(path2 -> Integer.parseInt(path2.toString().substring((config.getNetworkBaseDir() + "\\"  ).length()).replace(".params", "").replace(config.getModelName(), "").replace("-", "")))
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

}
