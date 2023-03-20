package ai.enpasos.muzero.platform.common;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

@Slf4j
public class FileUtils {
    private FileUtils() {
    }




    public static boolean exists(String pathStr) {
        Path path = Paths.get(pathStr);
        return Files.exists(path);
    }
    public static void rmDir(String pathStr) {

        try {
            Path path = Paths.get(pathStr);
            if (!Files.exists(path)) {
                return;
            }
            try(Stream<Path> s = Files.walk(Paths.get(pathStr))) {
                s.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }


    }

    public static void mkDir(String pathStr) {
        try {
            Files.createDirectories(Paths.get(pathStr));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }
    }

    public static void cp(String filePathStr, String targetDir) {
        try {
            Files.copy(Paths.get(filePathStr), Paths.get(targetDir),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }
    }
}
