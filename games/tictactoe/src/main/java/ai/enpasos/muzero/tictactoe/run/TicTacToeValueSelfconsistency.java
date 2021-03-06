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

package ai.enpasos.muzero.tictactoe.run;

import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.run.ValueSelfconsistency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class TicTacToeValueSelfconsistency {

    @Autowired
    ReplayBuffer replayBuffer;


    @Autowired
    ValueSelfconsistency valueSelfconsistency;


    @SuppressWarnings("squid:S3740")
    public void run() {

        replayBuffer.loadLatestState();
        valueSelfconsistency.run(true);

    }


}
