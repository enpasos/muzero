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

import ai.enpasos.muzero.platform.agent.Inference;
import ai.enpasos.muzero.tictactoe.config.TicTacToeConfigFactory;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.stream.Collectors;

public class TicTacToeInferenceHelper {

    private TicTacToeInferenceHelper() {}

    private static final String[] actionNames = {"a3", "b3", "c3", "a2", "b2", "c2", "a1", "b1", "c1"};

    public static String actionIndexToName(int i) {
        return actionNames[i];
    }

    public static int actionNameToIndex(String name) {
        return ArrayUtils.indexOf(actionNames, name.trim());
    }

    public static String aiDecision(List<String> actions, boolean withMCTS, String networkDir) {

        List<Integer> actionInts = actions.stream().map(TicTacToeInferenceHelper::actionNameToIndex).collect(Collectors.toList());
        return actionIndexToName(Inference.aiDecision(actionInts, withMCTS, networkDir, TicTacToeConfigFactory.getTicTacToeInstance()));
    }

}
