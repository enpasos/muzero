package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.run.TemperatureCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TicTacToeTemperature {

    @Autowired
    TemperatureCalculator temperatureCalculator;

    public void run() {
      //  temperatureCalculator.runOnTimeStepLevel(0);
      //  temperatureCalculator.aggregatePerEpoch();
       // temperatureCalculator.aggregatePerEpisode();
        temperatureCalculator.markArchived();
    }
}
