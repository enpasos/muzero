package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationOnePlayer;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationTwoPlayers;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.BitSet;


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


    int observationPartSize;
    byte[] observationPartA;
    byte[] observationPartB;


    public static class TimeStepDOBuilder {
        int observationPartSize;
        byte[] observationPartA;
        byte[] observationPartB;

        public TimeStepDOBuilder observation(Observation observation) {
            this.observationPartSize = observation.getPartSize();
            if (observation instanceof ObservationTwoPlayers) {
                ObservationTwoPlayers observationTwoPlayers = (ObservationTwoPlayers) observation;
                observationPartA = observationTwoPlayers.getPartA().toByteArray();
                observationPartB = observationTwoPlayers.getPartB().toByteArray();
            } else {
                ObservationOnePlayer observationOnePlayer = (ObservationOnePlayer) observation;
                observationPartA = observationOnePlayer.getPart().toByteArray();
            }
            return this;
        }


    }

    public Observation getObservation() {
        return observationPartB == null ?
                ObservationOnePlayer.builder()
                        .partSize(observationPartSize)
                        .part(BitSet.valueOf(observationPartA))
                        .build()
                :
                ObservationTwoPlayers.builder()
                        .partSize(observationPartSize)
                        .partA(BitSet.valueOf(observationPartA))
                        .partB(BitSet.valueOf(observationPartB))
                        .build();
    }

    public void setObservation(Observation observation) {
        if (observation instanceof ObservationOnePlayer) {
            ObservationOnePlayer observationOnePlayer = (ObservationOnePlayer) observation;
            observationPartSize = observationOnePlayer.getPartSize();
            observationPartA = observationOnePlayer.getPart().toByteArray();
        } else if (observation instanceof ObservationTwoPlayers) {
            ObservationTwoPlayers observationTwoPlayers = (ObservationTwoPlayers) observation;
            observationPartSize = observationTwoPlayers.getPartSize();
            observationPartA = observationTwoPlayers.getPartA().toByteArray();
            observationPartB = observationTwoPlayers.getPartB().toByteArray();
        } else {
            throw new RuntimeException("unknown observation type");
        }
    }

 float[] playoutPolicy;
 boolean[] legalActions;
 Float rootValueTarget;
 Float vMix;
 Float rootEntropyValueTarget;
 Float rootEntropyValueFromInitialInference;
 Float rootValueFromInitialInference;
 Float legalActionMaxEntropy;
}
