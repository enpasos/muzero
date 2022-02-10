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

import java.util.List;

/**
 * The environment the agent is interacting with by applying actions and receiving rewards and new observations
 * of whatever the environment reveals about its new internal states.
 */
public interface Environment {
    /**
     * applies an action and gets immediate reward
     *
     * @return immediate reward
     */
    float step(Action action);


    // the environment reveals the following information abouts its state
    int[][] currentImage();

    boolean terminal();

    List<Action> legalActions();


    // general information about the environment
    List<Action> allActionsInActionSpace();
}
