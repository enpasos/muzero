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

package ai.enpasos.muzero.tictactoe.config;

import ai.enpasos.muzero.platform.agent.d_model.NetworkIO;
import ai.enpasos.muzero.platform.agent.d_model.ObservationModelInput;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationTwoPlayers;
import ai.enpasos.muzero.platform.agent.e_experience.ZeroSumGame;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.c_planning.Node;
import ai.enpasos.muzero.platform.agent.b_episode.Player;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.EnvironmentBase;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.e_experience.Observation.bitSetToFloatArray;

@Slf4j
@SuppressWarnings("squid:S2160")
public class TicTacToeGame extends ZeroSumGame {

    public static final String PASS = "pass: ";


    public TicTacToeGame(@NotNull MuZeroConfig config, EpisodeDO episodeDO) {
        super(config, episodeDO);

    }

    public TicTacToeGame(@NotNull MuZeroConfig config) {
        super(config);
    }

    @Override
    public @NotNull TicTacToeEnvironment getEnvironment() {
        return (TicTacToeEnvironment) environment;
    }

    @Override
    public boolean terminal() {
        return this.getEnvironment().isTerminal();
    }


    @Override
    public List<Action> allActionsInActionSpace() {
        return IntStream.range(0, config.getActionSpaceSize()).mapToObj(i -> config.newAction(i)).collect(Collectors.toList());
    }


    public void replayToPositionInEnvironment(int stateIndex) {
        environment = new TicTacToeEnvironment(config);
        if (stateIndex == -1) return;
        for (int i = 0; i < stateIndex; i++) {
            Action action = config.newAction(this.getEpisodeDO().getActions().get(i));
            environment.step(action);
        }
    }


    @SuppressWarnings("squid:S2095")
    public @NotNull ObservationModelInput getObservationModelInput(int position) {

        int n = config.getNumObservationLayers() * config.getBoardHeight() * config.getBoardWidth();

        BitSet rawResult = new BitSet(n);

        OneOfTwoPlayer currentPlayer =  position % 2 == 0 ? OneOfTwoPlayer.PLAYER_A : OneOfTwoPlayer.PLAYER_B;

        int index = 0;
        ObservationTwoPlayers observation = (ObservationTwoPlayers)this.getEpisodeDO().getTimeSteps().get(position).getObservation();

        index = observation.addTo(currentPlayer, rawResult, index);

        float v = currentPlayer.getActionValue();
        for (int i = 0; i < config.getBoardHeight(); i++) {
            for (int j = 0; j < config.getBoardWidth(); j++) {
                rawResult.set(index++, v == 1f);
            }
        }

        return new ObservationModelInput(bitSetToFloatArray(n, rawResult), new long[]{config.getNumObservationLayers(), config.getBoardHeight(), config.getBoardWidth()});
    }


    @Override
    public Player toPlay() {
        return this.getEpisodeDO().getActions().size() % 2 == 0 ? OneOfTwoPlayer.PLAYER_A: OneOfTwoPlayer.PLAYER_B;
    }


    @Override
    public String render() {
        if (getEnvironment() == null)  return "no rendering when not connected to the environment";  // TODO: environment decoupling needed
        String r = this.getEpisodeDO().getActions().size() + ": ";
        OneOfTwoPlayer player = null;
        if (!this.getEpisodeDO().getActions().isEmpty()) {
            Action action = config.newAction(this.getEpisodeDO().getActions().get(this.getEpisodeDO().getActions().size() - 1));
            int colLastMove = ((TicTacToeAction) action).getCol();

            player = OneOfTwoPlayer.otherPlayer(this.getEnvironment().getPlayerToMove());
            r += player.getSymbol() + " move (" + (((TicTacToeAction) action).getRow() + 1) + ", " + (char) (colLastMove + 65) + ") index " + action.getIndex();
        }
        r += "\n";
        r += getEnvironment().render();
        int t = this.getEpisodeDO().getLastActionTime();
        if (terminal() && t > 0) {
            if (this.getEpisodeDO().getTimeSteps().get(t).getReward() == 0.0f) {
                r += "\ndraw";
            } else {
                r += "\nwinning: " + Objects.requireNonNull(player).getSymbol();
            }
            log.debug("\nG A M E  O V E R");
        }
        return r;
    }


    public void renderMCTSSuggestion(@NotNull MuZeroConfig config, float @NotNull [] policyTargets) {

        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];

        log.debug("\nmcts suggestion:");
        int boardSize = config.getBoardHeight() * config.getBoardWidth();
        for (int i = 0; i < boardSize; i++) {
            values[TicTacToeAction.getRow(config, i)][TicTacToeAction.getCol(config, i)] = String.format("%2d", Math.round(100.0 * policyTargets[i])) + "%";
        }
        log.debug(EnvironmentBase.render(config, values));
        if (policyTargets.length > boardSize) {
            log.debug(PASS + String.format("%2d", Math.round(100.0 * policyTargets[boardSize])) + "%");
        }
    }

    @Override
    public void connectToEnvironment() {
        environment = new TicTacToeEnvironment(config);
    }

    public void renderNetworkGuess(@NotNull MuZeroConfig config, @NotNull Player toPlay, @Nullable NetworkIO networkOutput, boolean gameOver) {
        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];
        if (networkOutput != null) {
            double v = networkOutput.getValue();
            double p = (v + 1) / 2 * 100;
            int percent = (int) Math.round(p);
            log.debug("\nnetwork guess:");
            if (!gameOver) {
                int boardSize = config.getBoardHeight() * config.getBoardWidth();
                for (int i = 0; i < boardSize; i++) {
                    values[TicTacToeAction.getRow(config, i)][TicTacToeAction.getCol(config, i)] = String.format("%2d", Math.round(100.0 * networkOutput.getPolicyValues()[i])) + "%";  // because softmax
                }
                log.debug(EnvironmentBase.render(config, values));
                if (networkOutput.getPolicyValues().length > boardSize) {
                    log.debug(PASS + String.format("%2d", Math.round(100.0 * networkOutput.getPolicyValues()[boardSize])) + "%");
                }

            }
            if (toPlay instanceof OneOfTwoPlayer oneOfTwoPlayer)
                log.debug("Estimated chance for " + oneOfTwoPlayer.getSymbol() + " to win: " + percent + "%");

        }
    }

    public void renderSuggestionFromPriors(@NotNull MuZeroConfig config, @NotNull Node node) {
        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];

        log.info("\nsuggestion from priors:");
        int boardSize = config.getBoardHeight() * config.getBoardWidth();
        for (int i = 0; i < boardSize; i++) {
            Action a = config.newAction(i);
            float value;
            value = (float) node.getChildren().stream().filter(n -> n.getAction().equals(a)).findFirst().orElse(new Node(config, 0f)).getPrior();
            values[TicTacToeAction.getRow(config, i)][TicTacToeAction.getCol(config, i)]
                = String.format("%2d", Math.round(100.0 * value)) + "%";
        }

        log.info(EnvironmentBase.render(config, values));
    }


}
