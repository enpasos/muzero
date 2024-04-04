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
 //       config.setNetworkBaseDir(config.getOutputDir() + "/networks");
            modelService.loadLatestModelOrCreateIfNotExisting().get();
      //  modelService.loadLatestModel().get();
        log.info("modelService.loadLatestModel().get()  ... done");
//
 //       config.setNetworkBaseDir(config.getOutputDir() + "/networksOutput");
        modelService.saveLatestModelParts(new boolean[] {true, true, true}).get();

//            config.setNetworkBaseDir(config.getOutputDir() + "/networksOutput");
//            modelService.loadLatestModelParts(new boolean[] {true, true, true}).get();
//            log.info("modelService.loadLatestModel().get()  ... done");

//            config.setNetworkBaseDir(config.getOutputDir() + "/networksOutput2");
//            modelService.saveLatestModelParts(new boolean[] {true, true, true}).get();
    } catch (InterruptedException e) {
        log.error("Interrupted", e);
        Thread.interrupted();
    } catch (Exception e) {
        throw new MuZeroException(e);
    }
}



}
