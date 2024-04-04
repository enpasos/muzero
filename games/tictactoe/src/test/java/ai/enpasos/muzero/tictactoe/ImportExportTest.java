package ai.enpasos.muzero.tictactoe;


import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.agent.d_model.djl.blocks.d_lowerlevel.CausalBroadcastResidualBlock;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;


@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
public class ImportExportTest   {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    ModelService modelService;



    @Test
    public void testImportExport() {
        log.info("ImportExportTest.run()");
        try {
            modelService.loadLatestModelOrCreateIfNotExisting().get();
            log.info("modelService.loadLatestModel().get()  ... done");
            config.setNetworkBaseDir(config.getOutputDir() + "/networksOutput");
            modelService.saveLatestModelParts(new boolean[]{true, true, true}).get();
            modelService.loadLatestModelParts(new boolean[]{true, true, true}).get();
            modelService.saveLatestModel().get();
            checkFilesAreTheSame(
                    config.getOutputDir() + "/networks" + "/MuZero-TicTacToe-0000.params",
                    config.getOutputDir() + "/networksOutput" + "/MuZero-TicTacToe-0000.params"
            );
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
            Thread.interrupted();
        } catch (Exception e) {
            throw new MuZeroException(e);
        }
    }

public void checkFilesAreTheSame(String pathToFile1, String pathToFile2) throws Exception{
    byte[] file1Bytes = Files.readAllBytes(Paths.get(pathToFile1));
    byte[] file2Bytes = Files.readAllBytes(Paths.get(pathToFile2));

    assertArrayEquals(file1Bytes, file2Bytes, "Files' binary content differs");

}



}
