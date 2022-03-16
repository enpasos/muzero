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

package ai.enpasos.muzero.platform.agent.rational;

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MCTS {

    @Autowired
    private MuZeroConfig config;


    public Action selectActionByMaxFromDistribution(List<Pair<Action, Double>> distributionInput) {
        Collections.shuffle(distributionInput);
        return distributionInput.stream()
            .max(Comparator.comparing(Pair::getSecond))
            .orElseThrow(MuZeroException::new).getKey();
    }


    @Nullable
    public List<NetworkIO> recurrentInference(@NotNull Network network, List<List<Node>> searchPathList) {
        List<Action> lastActions = searchPathList.stream().map(nodes -> nodes.get(nodes.size() - 1).getAction()).collect(Collectors.toList());
        List<NDArray> actionList = lastActions.stream().map(action ->
            network.getActionSpaceOnDevice().get(action.getIndex())
        ).collect(Collectors.toList());

        List<NDArray> hiddenStateList = searchPathList.stream().map(searchPath -> {
            Node parent = searchPath.get(searchPath.size() - 2);
            return parent.getHiddenState();
        }).collect(Collectors.toList());
        return network.recurrentInferenceListDirect(hiddenStateList, actionList);
    }


    private void clean(@NotNull Node node) {
        if (node.getHiddenState() != null) {
            node.getHiddenState().close();
            node.setHiddenState(null);
        }
        node.getChildren().forEach(this::clean);
    }

    private @NotNull String searchPathToString(@NotNull List<Node> searchPath, boolean withValue, MinMaxStats minMaxStats) {
        StringBuilder buf = new StringBuilder();
        searchPath.forEach(
            n -> {
                if (n.getAction() != null) {
                    buf.append(", ")
                        .append(n.getAction().getIndex());
                    if (withValue) {
                        buf.append("(")
                            // .append(n.valueScore(minMaxStats, config))
                            .append(n.qValue() + "")
                            .append(")");
                    }
                } else {
                    buf.append("root");
                }
            }
        );
        return buf.toString();
    }

}
