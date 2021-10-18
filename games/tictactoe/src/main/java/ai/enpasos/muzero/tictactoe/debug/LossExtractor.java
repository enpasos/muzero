package ai.enpasos.muzero.tictactoe.debug;

import ai.djl.Device;
import ai.djl.Model;
import ai.enpasos.muzero.MuZero;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.agent.fast.model.djl.blocks.atraining.MuZeroBlock;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Map;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.agent.fast.model.Network.getDoubleValue;
import static ai.enpasos.muzero.agent.fast.model.Network.getEpoch;
import static ai.enpasos.muzero.debug.LossExtractor.listLossesForTrainedNetworks;

public class LossExtractor {

    public static void main(String[] args) throws Exception {
        MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();
        config.setOutputDir("./memory/");
        listLossesForTrainedNetworks(config);
    }

}
