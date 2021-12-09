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

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import lombok.Data;

import java.util.SortedMap;
import java.util.TreeMap;

@Data
public class Node {
    private Player toPlay;
    private double prior;
    private SortedMap<Action, Node> children;
    private NDArray hiddenState;
    private double reward;
    private double multiplierLambda;
    private double valueSum;
    private Action action;
    private int visitCount;
    private boolean root = false;
    private MuZeroConfig config;

    public Node(MuZeroConfig config, double prior, boolean root) {
        this(config, prior);
        this.root = root;
    }

    public Node(MuZeroConfig config, double prior) {
        this.config = config;
        this.visitCount = 0;
        this.prior = prior;
        this.valueSum = 0;
        this.children = new TreeMap<>();
        hiddenState = null;
        reward = 0.0;
    }


    public boolean expanded() {
        return this.getChildren().size() > 0;
    }


    private double value() {
        if (visitCount == 0) return 0.0;
        double rawValue = this.getValueSum() / this.visitCount;
        if (config.getPlayerMode() == PlayerMode.TWO_PLAYERS) {
            return -rawValue;
        } else {
            return rawValue;
        }
    }


    public double valueScore(MinMaxStats minMaxStats, MuZeroConfig  config) {
        return minMaxStats.normalize(getReward() + config.getDiscount() * value());
    }

}
