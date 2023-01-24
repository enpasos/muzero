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

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.rational.GumbelSearch;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ConstantConditions")

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class SearchManagerTest {

    @Autowired
    MuZeroConfig config;

    @Test
    void searchManagerTest() {
        config.setNetworkBaseDir("./pretrained");
        int n = 200;
        config.setNumSimulations(  n);
        config.setCVisit(16);
        Game game = config.newGame();
        Objects.requireNonNull(game).apply(0, 3, 1, 4, 2);
        game.initSearchManager(0);
        GumbelSearch searchManager = game.getSearchManager();
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            MuZeroBlock block = new MuZeroBlock(config);
            model.setBlock(block);
            model.load(Paths.get(config.getNetworkBaseDir()));
            Network network = new Network(config, model);
            try (NDManager nDManager = network.getNDManager().newSubManager()) {
                List<NDArray> actionSpaceOnDevice = Network.getAllActionsOnDevice(config, nDManager);
                network.setActionSpaceOnDevice(actionSpaceOnDevice);
                network.createAndSetHiddenStateNDManager(nDManager, true);
                List<NetworkIO> networkOutput = network.initialInferenceListDirect(List.of(game));
                searchManager.expandRootNode(false, networkOutput.get(0));
                searchManager.gumbelActionsStart(true);
                for (int i = 0; i < 2 * n; i++) {
                    System.out.println("i:" + i + ", isSimulationsFinished?" + searchManager.isSimulationsFinished() + "... " + searchManager.getGumbelInfo());
                    assertTrue((searchManager.getGumbelInfo().isFinished() && i >= config.getNumSimulations( )) ||
                        (!searchManager.getGumbelInfo().isFinished() && i < config.getNumSimulations( )));
                    searchManager.next();
                }
            }
        } catch (MalformedModelException | IOException e) {
            e.printStackTrace();
        }

    }
}
