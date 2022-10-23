package ai.enpasos.mnist.inference;


import ai.djl.Model;
import ai.djl.engine.Engine;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.tictactoe.TicTacToe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
@Slf4j
@ComponentScan(basePackages = "ai.enpasos.*", excludeFilters = {
@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = TicTacToe.class)})
@SuppressWarnings("all")
public class DJLTicTacToeTest implements CommandLineRunner {


    @Autowired
    private MuZeroConfig conf;

    public static void main(String[] args) {
        SpringApplication.run(DJLTicTacToeTest.class, args);
    }


    @Override
    public void run(String... args) {
        log.info("default engine name: " + Engine.getDefaultEngineName());
        log.info("all engines: " + Engine.getAllEngines());
        String modelPath = "./models/initialInference.onnx";


        Model model = Model.newInstance("model", "OnnxRuntime");

        try (InputStream is = Files.newInputStream(Paths.get(modelPath))) {
            model.load(is);

            var predictor = model.newPredictor(new NaiveInitialInferenceListTranslator());
            try {
                Game game = conf.newGame();
                var result = predictor.predict(List.of(game));


            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            //           log.info("{} wrong classified images in {} non trained testimages", errors_total[0], errors_total[1]);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
