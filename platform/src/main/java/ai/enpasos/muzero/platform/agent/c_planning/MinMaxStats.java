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

package ai.enpasos.muzero.platform.agent.c_planning;


public class MinMaxStats {

    private double maximum;
    private double minimum;


    public MinMaxStats(KnownBounds knownBounds) {
        this();
        if (knownBounds != null) {
            this.minimum = knownBounds.min;
            this.maximum = knownBounds.max;
        }
    }

    public MinMaxStats(double minimum, double maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public MinMaxStats() {
        this.minimum = Double.MAX_VALUE;
        this.maximum = -Double.MAX_VALUE;
    }

    public void update(double value) {
        this.maximum = Math.max(this.maximum, value);
        this.minimum = Math.min(this.minimum, value);
    }


    public double normalize(double value) {
        if (this.maximum > this.minimum) {
            return (value - this.minimum) / (this.maximum - this.minimum);
        }
        return value;
    }
}
