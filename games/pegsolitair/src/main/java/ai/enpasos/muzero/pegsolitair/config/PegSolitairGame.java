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

package ai.enpasos.muzero.pegsolitair.config;

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.ObservationModelInput;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.agent.b_episode.Player;
import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.e_experience.Observation.bitSetToFloatArray;

public class PegSolitairGame extends Game {


    public PegSolitairGame(@NotNull MuZeroConfig config, EpisodeDO episodeDO) {
        super(config, episodeDO);

    }

    public PegSolitairGame(@NotNull MuZeroConfig config) {
        super(config);

    }

    @Override
    public @NotNull PegSolitairEnvironment getEnvironment() {
        return (PegSolitairEnvironment) environment;
    }

    @Override
    public boolean terminal() {
        return this.getEnvironment().isTerminal();
    }




    @Override
    public List<Action> allActionsInActionSpace() {
        return IntStream.range(0, config.getActionSpaceSize()).mapToObj(i -> config.newAction(i)).collect(Collectors.toList());
    }




    public @NotNull ObservationModelInput getObservationModelInput(int inputTime) {

        int n = config.getNumObservationLayers() * config.getBoardHeight() * config.getBoardWidth();
        BitSet rawResult = new BitSet(n);

        Observation observation = episodeDO.getTimeStep(inputTime).getObservation();
         observation.addTo(null, rawResult, 0 );

        return new ObservationModelInput(bitSetToFloatArray(n, rawResult), new long[]{config.getNumObservationLayers(), config.getBoardHeight(), config.getBoardWidth()});

    }


    @Override
    public void replayToPositionInEnvironment(int stateIndex) {
        environment = new PegSolitairEnvironment(config);
        if (stateIndex == -1) return;
        for (int i = 0; i < stateIndex; i++) {
            Action action = config.newAction(this.getEpisodeDO().getTimeSteps().get(i).getAction() );
            environment.step(action);
        }
    }



    @Override
    public Player toPlay() {
        return null;
    }

    @Override
    public String render() {
        if (getEnvironment() == null)  return "no rendering when not connected to the environment";
        return ((PegSolitairEnvironment) environment).render();
    }


    public void renderMCTSSuggestion(@NotNull MuZeroConfig config, float @NotNull [] childVisits) {
        // not implemented, yet
    }

    @Override
    public void connectToEnvironment() {
        environment = new PegSolitairEnvironment(config);
    }

    public void renderNetworkGuess(@NotNull MuZeroConfig config, Player toPlay, @Nullable NetworkIO networkOutput, boolean gameOver) {
        // not implemented, yet
    }

    public void renderSuggestionFromPriors(@NotNull MuZeroConfig config, @NotNull Node node) {
        // not implemented, yet
    }
}
