package ai.enpasos.muzero.go.run;


import ai.enpasos.muzero.go.run.test.GoTest;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZeroLoop;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
public class GoTestComponent {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private GoTest goTest;

    @Autowired
    private MuZeroLoop muZero;

    public void run() {
       // List<Integer> startPosition = List.of(12, 16, 17, 6, 11, 7, 8, 3, 13, 10);
      //  List<Integer> startPosition = List.of(12,16,17,6,11,7,8,3,13,10,22,19,15,20,21,15,5,15,1,18,2,14,7);
        List<Integer> startPosition = List.of(12, 16, 17, 6, 11, 7, 8, 3, 13, 10, 15, 20, 5, 0, 9, 22, 18, 2, 23, 19, 21, 24, 15, 5, 14, 24, 4, 25, 1, 2, 6, 5);



            boolean passed = goTest.findBadDecisions(startPosition) == 0;
        String message = "INTEGRATIONTEST = " + (passed ? "passed" : "failed");
        log.info(message);

    }


}
