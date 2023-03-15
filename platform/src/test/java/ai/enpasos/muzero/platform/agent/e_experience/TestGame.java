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

package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.ObservationModelInput;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.episode.Player;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
@SuppressWarnings("squid:S2160")
public class TestGame extends Game {


    public TestGame(@NotNull MuZeroConfig config) {
        super(config);
        this.environment = new TestEnvironment(config);
    }


    @Override
    public boolean terminal() {
        return false;
    }

    @Override
    public List<Action> legalActions() {
        return null;
    }

    @Override
    public List<Action> allActionsInActionSpace() {
        return null;
    }

    @Override
    public Player toPlay() {
        return null;
    }

    @Override
    public String render() {
        return null;
    }

    @Override
    public ObservationModelInput getObservationModelInput() {
        return null;
    }

    @Override
    public void replayToPosition(int stateIndex) {

    }

    @Override
    public void renderNetworkGuess(MuZeroConfig config, Player toPlay, NetworkIO networkIO, boolean b) {

    }

    @Override
    public void renderSuggestionFromPriors(MuZeroConfig config, Node node) {

    }

    @Override
    public void renderMCTSSuggestion(MuZeroConfig config, float[] childVisits) {

    }

    @Override
    public void initEnvironment() {

    }
}
