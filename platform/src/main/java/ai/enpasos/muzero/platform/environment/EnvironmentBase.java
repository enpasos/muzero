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

package ai.enpasos.muzero.platform.environment;


import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.List;


@Data
@NoArgsConstructor
public class EnvironmentBase implements Environment {


    private static final String X_COORD = " A   B   C   D   E   F   G   H   I   J   K   L   M   N   O   P   Q   R   S   T   U   V   W   X   Y   Z ";
    private static final String X_COORD_SMALL = " A B C D E F G H I J K L M N O P Q R S T U V W X Y Z ";
    protected int[][] board;
    protected MuZeroConfig config;


    public EnvironmentBase(@NotNull MuZeroConfig config) {
        this.config = config;
        board = new int[config.getBoardHeight()][config.getBoardWidth()];
    }

    public static @NotNull String render(@NotNull MuZeroConfig config, String[][] values) {

        String v = "|";

        boolean smallSpacing = values[0][0].length() <= 1;
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (int j = 0; j < config.getBoardHeight(); j++) {
            sb.append(config.getBoardHeight() - j);
            sb.append(v);
            for (int i = 0; i < config.getBoardWidth(); i++) {
                String value = values[config.getBoardHeight() - 1 - j][i];
                if (" 0%".equals(value)) {
                    value = "   ";
                }
                sb.append(value);
                sb.append(v);
            }
            sb.append("\n");
        }
        if (smallSpacing) {
            sb.append(" ").append(X_COORD_SMALL, 0, config.getSize() * 2).append("\n");
        } else {
            sb.append("  ").append(X_COORD, 0, config.getSize() * 4).append("\n");
        }


        return sb.toString();

    }

    public float step(Action action) {
        throw new NotImplementedException("step() not implemented, yet.");
    }

    @Override
    public int[][] currentImage() {
        throw new NotImplementedException("currentImage() not implemented, yet.");
    }

    @Override
    public boolean terminal() {
        throw new NotImplementedException("terminal() not implemented, yet.");
    }

    @Override
    public @NotNull List<Action> legalActions() {
        throw new NotImplementedException("legalActions() not implemented, yet.");
    }

    @Override
    public @NotNull List<Action> allActionsInActionSpace() {
        throw new NotImplementedException("allActionsInActionSpace() not implemented, yet.");
    }


}
