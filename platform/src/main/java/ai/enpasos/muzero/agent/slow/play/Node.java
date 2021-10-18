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

package ai.enpasos.muzero.agent.slow.play;

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.MuZeroConfig;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.SortedMap;
import java.util.TreeMap;

@Data
public class Node {
    public @Nullable Player toPlay;
    public double prior;
    public double multiplierLambda;
    public double valueSum;
    public SortedMap<Action, Node> children;
    public @Nullable NDArray hiddenState;
    public Action action;
    public double reward;
    private int visitCount;
    private boolean root = false;

    public Node(double prior, boolean root) {
        this(prior);
        this.root = root;
    }

    public Node(double prior) {
        this.visitCount = 0;

        this.prior = prior;
        this.valueSum = 0;
        this.children = new TreeMap<>();
        hiddenState = null;
        reward = 0.0;
    }


    public boolean expanded() {
        return this.children.size() > 0;
    }


    private double value() {
        if (visitCount == 0) return 0.0;
        return -this.valueSum / this.visitCount;
    }


    public double valueScore(MinMaxStats minMaxStats, MuZeroConfig config) {
        return minMaxStats.normalize(getReward() + config.getDiscount() * value());
    }

}
