package ai.enpasos.muzero.platform.common;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

@Slf4j
public class FileUtils2 {
    private FileUtils2() {
    }




    public static boolean exists(String pathStr) {
        Path path = Paths.get(pathStr);
        return Files.exists(path);
    }
    public static void rmDir(String pathStr) {
        // implement deleting directory from path string using guava dependency
     //   MoreFiles.deleteRecursively(Paths.get(pathStr), RecursiveDeleteOption.ALLOW_INSECURE);

        try {
            Path path = Paths.get(pathStr);
            if (!Files.exists(path)) {
                return;
            }
            Files.walk(Paths.get(pathStr))
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }

//        try {
//            org.apache.commons.io.FileUtils.deleteDirectory(new File(pathStr));
//        } catch (IOException e) {
//            log.error(e.getMessage(), e);
//            throw new MuZeroException(e);
//        }
    }

    public static void mkDir(String pathStr) {
        try {
            Files.createDirectories(Paths.get(pathStr));
          //  org.apache.commons.io.FileUtils.forceMkdir(new File(pathStr));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }
    }

    public static void cp(String filePathStr, String targetDir) {
        try {
            Files.copy(Paths.get(filePathStr), Paths.get(targetDir),
                StandardCopyOption.REPLACE_EXISTING);
            //org.apache.commons.io.FileUtils.copyFileToDirectory(new File(filePathStr), new File(targetDir));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }
    }
}
