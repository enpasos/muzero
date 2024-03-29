/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ai.enpasos.muzero.tictactoe.run;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.GameBufferDTO;
import ai.enpasos.muzero.platform.agent.e_experience.GameBufferIO;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.EntropyExtractor;
import ai.enpasos.muzero.platform.run.GameProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;

import static ai.enpasos.muzero.platform.agent.e_experience.GameBuffer.getEpochFromPath;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class TicTacToeEntropyExtractor {
    @Autowired
    MuZeroConfig config;

    @Autowired
    EntropyExtractor entropyExtractor;


    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    GameBufferIO gameBufferIO;


    @Autowired
    GameProvider surpriseExtractor;

    public void runA() {
        List<Integer> actionIndexList = surpriseExtractor.getGameStartingWithActionsFromStart(0, 4, 7, 3, 5, 6, 2, 1, 8).orElseThrow(MuZeroException::new).getGameDTO().getActions();
        System.out.println(entropyExtractor.listValuesForTrainedNetworks(actionIndexList));
    }



    public void run(boolean onlyInitialState) {

        List<Path> paths = this.gameBufferIO.getBufferNames();
        Collections.reverse(paths);
        int epochMax = 0;
        Map<Integer, Double> entropyMap = new HashMap<>();
        for (int h = 0; h < paths.size(); h++) {
            Path path = paths.get(paths.size() - 1 - h);
            GameBufferDTO gameBufferDTO = this.gameBufferIO.loadState(path);
            int epoch = getEpochFromPath(path);
            double entropy = gameBufferDTO.getGames().stream().mapToDouble(game ->
                    {
                       if (onlyInitialState) {
                           return game.getGameDTO().getEntropyOfInitialState();
                       }   else {
                           return game.getGameDTO().getAverageEntropy();
                       }
                    }
            ).average().orElse(0d);

            entropyMap.put(epoch, entropy);
           // System.out.println(epoch + ";" + entropy);

        }
        entropyMap.forEach((epoch, entropy) -> System.out.println(epoch + ";" +NumberFormat.getNumberInstance().format(entropy)));
    }

}
