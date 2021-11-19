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

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.pegsolitair.config.environment.Board;
import ai.enpasos.muzero.pegsolitair.config.environment.Point;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.fast.model.Observation;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.gamebuffer.GameDTO;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.agent.slow.play.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PegSolitairGame extends Game {


    public PegSolitairGame(@NotNull MuZeroConfig config, GameDTO gameDTO) {
        super(config, gameDTO);
        environment = new PegSolitairEnvironment(config);
    }

    public PegSolitairGame(@NotNull MuZeroConfig config) {
        super(config);
        environment = new PegSolitairEnvironment(config);
    }

    public @NotNull PegSolitairEnvironment getEnvironment() {
        return (PegSolitairEnvironment) environment;
    }

    @Override
    public boolean terminal() {
        return this.getEnvironment().terminal();
    }


    @Override
    public List<Action> legalActions() {
        return this.getEnvironment().legalActions();
    }

    @Override
    public List<Action> allActionsInActionSpace() {
        return IntStream.range(0, config.getActionSpaceSize()).mapToObj(i -> new Action(config, i)).collect(Collectors.toList());
    }


    public void replayToPosition(int stateIndex) {
        environment = new PegSolitairEnvironment(config);
        if (stateIndex == -1) return;
        for (int i = 0; i < stateIndex; i++) {
            Action action = new Action(config, this.getGameDTO().getActionHistory().get(i));
            environment.step(action);
        }
    }



    public @NotNull Observation getObservation(@NotNull NDManager ndManager) {



        // values in the range [0, 1]
//        NDArray boardCurrentPlayer = ndManager.create(getBoardPositions(this.getEnvironment().currentImage(), currentPlayer.getValue()));
////        NDArray boardOpponentPlayer = ndManager.create(getBoardPositions(this.getEnvironment().currentImage(), opponentPlayer.getValue()));
//
//        NDArray boardColorToPlay = ndManager.full(new Shape(config.getBoardHeight(), config.getBoardWidth()), currentPlayer.getActionValue());
//
//        NDArray stacked = NDArrays.stack(new NDList(boardCurrentPlayer, boardOpponentPlayer, boardColorToPlay));
        NDArray stacked = null;

        return new Observation(stacked);
    }

    private float[] @NotNull [] getBoardPositions(Board board, int p) {
        for (int row = 1; row <= 7; row++) {
            for (int col = 1; col <= 7; col++) {
                Point point = new Point(row, col);
            }
        }
       // return boardtransfer;
        return null;
    }


    @Override
    public Player toPlay() {
        return null;
    }

    @Override
    public String render() {

//        String r = this.getGameDTO().getActionHistory().size() + ": ";
//        OneOfTwoPlayer player = null;
//        if (this.getGameDTO().getActionHistory().size() > 0) {
//            Action action = new Action(config, this.getGameDTO().getActionHistory().get(this.getGameDTO().getActionHistory().size() - 1));
//            int colLastMove = action.getCol();
//
//            player = OneOfTwoPlayer.otherPlayer(this.getEnvironment().getPlayerToMove());
//            r += player.getSymbol() + " move (" + (action.getRow() + 1) + ", " + (char) (colLastMove + 65) + ") index " + action.getIndex();
//        }
//        r += "\n";
//        r += getEnvironment().render();
//        if (terminal() && this.getGameDTO().getRewards().size() > 0) {
//            if (this.getGameDTO().getRewards().get(this.getGameDTO().getRewards().size() - 1) == 0.0f) {
//                r += "\ndraw";
//            } else {
//                r += "\nwinning: " + Objects.requireNonNull(player).getSymbol();
//            }
//            System.out.println("\nG A M E  O V E R");
//        }
//        return r;
        return null;
    }
}
