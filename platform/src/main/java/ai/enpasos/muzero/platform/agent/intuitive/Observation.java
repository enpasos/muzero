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

package ai.enpasos.muzero.platform.agent.intuitive;

import ai.djl.Device;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Data
@EqualsAndHashCode
public class Observation {
    private float[] value;
    private long[] shape;

    public Observation(float[] value, long[] shape) {
        this.value = value;
        this.shape = shape;
    }

    public NDArray getNDArray(@NotNull NDManager ndManager) {
        return ndManager.create(value).reshape(shape);
    }





//    public static void  main(String[] args) {
//
//        // create a java float array length 1000 filled with random values
//        float[] data = new float[1000];
//        for (int i = 0; i < data.length; i++) {
//            data[i] = (float) Math.random();
//        }
//        // convert float array to byte[] result using ByteArrayOutputStream
//          ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            DataOutputStream dos = new DataOutputStream(baos);
//            try {
//                for (float f : data) {
//                    dos.writeFloat(f);
//                }
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            byte[] result = baos.toByteArray();
//            System.out.println("with Java only: " +result.length);
//
//
//        NDManager manager = NDManager.newBaseManager(Device.cpu());
//
//        byte[] result2 = manager.create(data).encode();
//        System.out.println("with NDManager: " + result2.length);
//
//
//    }
}
