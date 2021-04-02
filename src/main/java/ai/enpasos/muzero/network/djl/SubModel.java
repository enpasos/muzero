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

package ai.enpasos.muzero.network.djl;

import ai.djl.BaseModel;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.ndarray.types.DataType;
import ai.djl.nn.Block;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;


@Slf4j
public class SubModel extends BaseModel {
    public SubModel(String modelName, Model model, Block block) {
        super(modelName);
        super.manager = model.getNDManager().newSubManager();
        super.setBlock(block);
    }


    @Override
    public void load(Path modelPath) throws IOException, MalformedModelException {
        log.error("load1 is not implemented #### not expected to be called");
        throw new NotImplementedException("load not implemented on SubModel (use Model)");
    }

    @Override
    public void load(Path modelPath, String prefix) throws IOException, MalformedModelException {
        log.error("load2 is not implemented #### not expected to be called");
        throw new NotImplementedException("load not implemented on SubModel (use Model)");
    }

    @Override
    public void load(Path modelPath, String prefix, Map<String, ?> options) throws IOException, MalformedModelException {
        log.error("load3 is not implemented #### not expected to be called");
        throw new NotImplementedException("load not implemented on SubModel (use Model)");
    }


    @Override
    public void cast(DataType dataType) {
        log.error("cast is not implemented #### not expected to be called");
        throw new NotImplementedException("cast not implemented on SubModel (use Model)");
    }

    @Override
    public void quantize() {
        log.error("quantize is not implemented #### not expected to be called");
        throw new NotImplementedException("quantize not implemented on SubModel (use Model)");
    }

    @Override
    public void close() {
        log.error("close is not implemented #### not expected to be called");
        throw new NotImplementedException("close not implemented on SubModel (use Model)");
    }


}
