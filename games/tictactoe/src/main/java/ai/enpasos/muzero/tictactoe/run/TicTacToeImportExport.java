package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.FillValueTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TicTacToeImportExport {

//    @Autowired
//    FillValueTable fillValueTable;
    @Autowired
    MuZeroConfig config;

    @Autowired
    ModelService modelService;

    public void run() {
        log.info("TicTacToeImportExport.run()");
        try {
            config.setNetworkBaseDir(config.getOutputDir() + "/networks");
            modelService.loadLatestModel().get();
            log.info("modelService.loadLatestModel().get()  ... done");

            config.setNetworkBaseDir(config.getOutputDir() + "/networksOutput");
            modelService.saveLatestModel(new boolean[] {false, false, true}).get();

        } catch (InterruptedException e) {
            log.error("Interrupted", e);
            Thread.interrupted();
        } catch (Exception e) {
            throw new MuZeroException(e);
        }
    }
}
