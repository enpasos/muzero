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

package ai.enpasos.muzero.go.config;

import ai.enpasos.muzero.go.config.environment.GameState;
import ai.enpasos.muzero.go.config.environment.basics.Player;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import ai.enpasos.muzero.go.config.environment.basics.move.Pass;
import ai.enpasos.muzero.go.config.environment.basics.move.Play;
import ai.enpasos.muzero.go.config.environment.basics.move.Resign;
import ai.enpasos.muzero.go.config.environment.scoring.GameResult;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.EnvironmentZeroSumBase;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.go.config.GoAdapter.translate;


@Slf4j
@SuppressWarnings("squid:S2160")
public class GoEnvironment extends EnvironmentZeroSumBase {

    private final List<GameState> history;
    private GameState state;
    private GameResult result;

    public GoEnvironment(@NotNull MuZeroConfig config) {
        super(config);
        history = new ArrayList<>();
        state = GameState.newGame(config.getBoardWidth());
        history.add(state);
    }

    @Override
    public @NotNull List<Action> legalActions() {
        return state.getValidMoves().stream()
                .filter(m -> !(m instanceof Resign))  // muzero is not resigning :-)
                .map(move -> translate(this.config, move)).collect(Collectors.toList());
    }


    List<GameState> getHistory() {
        return this.history;
    }


    @Override
    public float step(@NotNull Action action) {

        Player thisPlayer = state.getNextPlayer();

        state = state.applyMove(translate(this.config, action));
        history.add(state);

        float reward = 0f;

        if (terminal()) {
            setResult(GameResult.apply(state.getBoard(), this.getConfig().getKomi()));
            log.debug(getResult().toString());
            reward = (thisPlayer == getResult().winner()) ? 1f : -1f;
        }

        return reward;
    }

    @Override
    public void swapPlayer() {
        throw new NotImplementedException("swapPlayer() is not implemented here");
    }


    @Override
    public OneOfTwoPlayer getPlayerToMove() {
        return translate(this.state.getNextPlayer());
    }

    @Override
    public void setPlayerToMove(OneOfTwoPlayer player) {
        throw new NotImplementedException("setPlayerToMove is not implemented");
    }

    @Override
    public int[][] currentImage() {
         throw new NotImplementedException("swapPlayer() is not implemented");
    }
    @Override
    public boolean terminal() {
        return this.state.isOver();
    }
    @Override
    public boolean hasPlayerWon(OneOfTwoPlayer player) {
        if (!terminal()) return false;
        return player == translate(getResult().winner());
    }


    public @NotNull String render() {

        String lastMove = "NONE YET";
        if (state.getLastMove() != null) {
            if (state.getLastMove() instanceof Pass) {
                lastMove = "PASS";
            } else if (state.getLastMove() instanceof Resign) {
                lastMove = "RESIGN";
            } else if (state.getLastMove() instanceof Play) {
                Play play = (Play) state.getLastMove();
                Point p = play.getPoint();
                lastMove = "PLAY(" + (char) (64 + p.getCol()) + ", " + (config.getBoardHeight() - p.getRow() + 1) + ")";
            }
        }

        String lastMoveStr = (state.getNextPlayer().other() == Player.BLACK_PLAYER ? "x" : "o")
                + " move: " + lastMove;

        String status = "GAME RUNNING";

        if (getResult() != null) {
            status = "GAME OVER\n" + getResult().toString();
        }

        return lastMoveStr + "\n" + state.getBoard().toString() + "\n" + status;

    }


    public GameResult getResult() {
        return result;
    }

    public void setResult(GameResult result) {
        this.result = result;
    }
}
