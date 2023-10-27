package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.muzero.platform.common.Functions.nonSimilarity;

@Entity
@Table(name = "statenode")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StateNodeDO {

    @OneToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH}, mappedBy = "statenode")
    @Builder.Default
    private List<TimeStepDO> timeSteps = new ArrayList<>();


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    //   @Column(unique = true)
    float[] simState;


    boolean[] legalActions;

    boolean[] visitedActions;
//
    boolean[] deeplyVisitedActions;


    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof StateNodeDO)) return false;
        final StateNodeDO other = (StateNodeDO) o;
        if (!other.canEqual((Object) this)) return false;

        if (!java.util.Arrays.equals(this.getLegalActions(), other.getLegalActions())) return false;
        if (java.util.Arrays.equals(this.getSimState(), other.getSimState())) return true;
        if (nonSimilarity(this.getSimState(), other.getSimState()) < 1e-16) return true;
        return false;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof StateNodeDO;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
       // result = result * PRIME + java.util.Arrays.hashCode(this.getSimState());
        result = result * PRIME + java.util.Arrays.hashCode(this.getLegalActions());
        return result;
    }
}
