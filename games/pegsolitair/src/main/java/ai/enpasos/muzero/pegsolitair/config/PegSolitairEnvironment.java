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

import ai.enpasos.muzero.pegsolitair.config.environment.Board;
import ai.enpasos.muzero.pegsolitair.config.environment.Jump;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.environment.Environment;
import lombok.Data;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class PegSolitairEnvironment implements Environment, Serializable {

    private final static Logger logger = LoggerFactory.getLogger(PegSolitairEnvironment.class);


    private Board board;

    public PegSolitairEnvironment(@NotNull MuZeroConfig config) {
        this.config = config;
        board = new Board();
    }



    public float step(@NotNull Action action) {

        Jump jump = ActionAdapter.getJump(action);
        board.applyJump(jump);

        if (board.getLegalJumps().size() == 0) {
            return board.getScore();
        } else {
            return 0f;
        }

    }

    @Override
     public @NotNull List<Action> legalActions() {
        return board.getLegalJumps().stream()
                .map(j -> ActionAdapter.getAction(config,j))
                .collect(Collectors.toList());

    }

    @Override
    public int[][] currentImage() {
        return null;
    }

    @Override
    public boolean terminal() {
        return false;
    }




    public @NotNull String render() {
        return board.render();
    }


    public transient MuZeroConfig config;


    @Override
    public @NotNull List<Action> allActionsInActionSpace() {
        throw new NotImplementedException("allActionsInActionSpace() not implemented, yet.");
    }


}
