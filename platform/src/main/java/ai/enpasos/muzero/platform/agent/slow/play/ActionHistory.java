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

package ai.enpasos.muzero.platform.agent.slow.play;


import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class ActionHistory implements Cloneable {


    private final @NotNull List<Integer> actions;
    private final int actionSpaceSize;
    private final MuZeroConfig config;


    public ActionHistory(MuZeroConfig config, @NotNull List<Integer> history, int actionSpaceSize) {
        this.config = config;
        this.actions = new ArrayList<>();
        this.actions.addAll(history);
        this.actionSpaceSize = actionSpaceSize;
    }

    public @NotNull List<Integer> getActionIndexList() {
        return actions;
    }


    public @NotNull ActionHistory clone() {
        return new ActionHistory(config, actions, actionSpaceSize);
    }


    public void addAction(@NotNull Action action) {
        this.actions.add(action.getIndex());
    }


    public @NotNull Action lastAction() {
        return new Action(config, actions.get(actions.size() - 1));
    }


    public static @NotNull List<Action> actionSpace(MuZeroConfig config) {
        List<Action> actions = new ArrayList<>();
        for (int i = 0; i < config.getActionSpaceSize(); i++) {
            actions.add(new Action(config, i));
        }
        return actions;
    }


    public @NotNull Player toPlay() {
        int t = this.actions.size();
        if (t % 2 == 0) return OneOfTwoPlayer.PlayerA;
        else return OneOfTwoPlayer.PlayerB;

    }
}
