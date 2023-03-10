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

package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.c_planning.GumbelSearch;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static ai.enpasos.muzero.platform.common.FileUtils.rmDir;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ConstantConditions")

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class SearchManagerTest {

    @Autowired
    MuZeroConfig config;

    @Autowired
    ModelService modelService;


    private void init() throws InterruptedException, ExecutionException {
        config.setOutputDir("./build/tictactoeTest/");
        rmDir(config.getOutputDir());
        modelService.loadLatestModelOrCreateIfNotExisting().get();
    }

    @Test
    void searchManagerTest() {
        int n = 200;
        config.setNumSimulations(  n);
        config.setCVisit(16);
        Game game = config.newGame();
        Objects.requireNonNull(game).apply(0, 3, 1, 4, 2);
        game.initSearchManager(0);
        GumbelSearch searchManager = game.getSearchManager();
        NetworkIO networkIO = modelService.initialInference(game).join();
        searchManager.expandRootNode(false, networkIO);
        searchManager.gumbelActionsStart(true);
        for (int i = 0; i < 2 * n; i++) {
            System.out.println("i:" + i + ", isSimulationsFinished?" + searchManager.isSimulationsFinished() + "... " + searchManager.getGumbelInfo());
            assertTrue((searchManager.getGumbelInfo().isFinished() && i >= config.getNumSimulations()) ||
                (!searchManager.getGumbelInfo().isFinished() && i < config.getNumSimulations()));
            searchManager.next();
        }


    }
}
