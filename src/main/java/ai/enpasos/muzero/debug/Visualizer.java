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

package ai.enpasos.muzero.debug;

import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.play.Action;

public class Visualizer {
    public static void main(String[] args) {

        MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();

        Game game = config.newGame();
        applyAction(config, game, 0);
        applyAction(config, game, 5);
        applyAction(config, game, 8);
        applyAction(config, game, 7);
        applyAction(config, game, 3);
        applyAction(config, game, 1);
        applyAction(config, game, 2);
        applyAction(config, game, 6);
        applyAction(config, game, 4);


//        ReplayBuffer replayBuffer = new ReplayBuffer(config);
//        replayBuffer.loadLatestState();   // at least there is one config now
//            Game game = config.newGame();
//            game.setGameDTO(replayBuffer.getBuffer().getData().values().iterator().next());
//        renderGame(config, game);
//        NDManager ndManager = NDManager.newBaseManager();
//
//            int numUnrollSteps = 1;
//        Sample sample = ReplayBuffer.sampleFromGame(numUnrollSteps, 42, game, 2, ndManager);
//
//
//        NDArray observation =    sample.getObservation().getNDArray(ndManager);
//        // ok
//
//
//
//
//       //     List<Sample> batch = List.of( sample );
//    //    List<NDArray> output = Trainer.constructOutput(ndManager, numUnrollSteps, batch);
//
//   //     NDList input = Trainer.constructInput(batch);
//            int i = 42;


    }

    private static void applyAction(MuZeroConfig config, Game game, int a) {
        game.apply(a);
        System.out.println("action=" + a + ", terminal=" + game.terminal() + ", " + game.legalActions());
    }

    public static void renderGame(MuZeroConfig config, Game game) {
        Game replayGame = config.newGame();

        System.out.println("\n" + replayGame.render());
        for (int i = 0; i < game.getGameDTO().getActionHistory().size(); i++) {
            Action action = new Action(config, game.getGameDTO().getActionHistory().get(i));
            replayGame.apply(action);
            System.out.println("\n" + replayGame.render());
        }
        // System.out.println(game.getEnvironment().)
    }


}
