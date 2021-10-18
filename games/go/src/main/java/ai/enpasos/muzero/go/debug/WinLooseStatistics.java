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

package ai.enpasos.muzero.go.debug;

import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.go.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import static ai.enpasos.muzero.platform.debug.WinLooseStatistics.winLooseStatisticsOnGamesInStoredBuffers;

@Slf4j
public class WinLooseStatistics {

    public static void main(String[] args) {

        MuZeroConfig config = ConfigFactory.getGoInstance(5);
        int start = 10000;

        winLooseStatisticsOnGamesInStoredBuffers(config, start);

    }

}
