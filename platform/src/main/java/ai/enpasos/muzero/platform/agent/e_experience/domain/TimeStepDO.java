package ai.enpasos.muzero.platform.agent.e_experience.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "timestep")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimeStepDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;


    @ManyToOne
    EpisodeDO episode;



    int t;
    Integer action;


//    @EqualsAndHashCode.Include
//    private List<Integer> actions;
 Float  reward ;
 Float  entropy;
 float[] policyTarget;
 // Observation observations;
 float[] playoutPolicy;
 boolean[] legalActions;
 Float rootValueTargets;
 Float vMix;
 Float rootEntropyValueTarges;
 Float rootEntropyValuesFromInitialInference;
 Float rootValuesFromInitialInference;
 Float legalActionMaxEntropy;
}
