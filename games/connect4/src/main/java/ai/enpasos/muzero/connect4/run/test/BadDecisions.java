package ai.enpasos.muzero.connect4.run.test;

import lombok.Data;

@Data
public class BadDecisions {
    int modelBased;
    int planningBased;

    public BadDecisions(int modelBased, int planningBased) {
        this.modelBased = modelBased;
        this.planningBased = planningBased;
    }

    public int total() {
        return modelBased + planningBased;
    }
}
