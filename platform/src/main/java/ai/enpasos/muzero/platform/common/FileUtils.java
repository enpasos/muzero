package ai.enpasos.muzero.platform.common;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            org.apache.commons.io.FileUtils.deleteDirectory(new File(pathStr));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }
    }

    public static void mkDir(String pathStr) {
        try {
            org.apache.commons.io.FileUtils.forceMkdir(new File(pathStr));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }
    }

    public static void cp(String filePathStr, String targetDir) {
        try {
            org.apache.commons.io.FileUtils.copyFileToDirectory(new File(filePathStr), new File(targetDir));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }
    }
}
