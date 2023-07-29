package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationOnePlayer;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationTwoPlayers;
import jakarta.persistence.*;
import lombok.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;


@Entity
@Table(name = "timestep", uniqueConstraints =
@UniqueConstraint(name = "UniqueEpisodeIDandTime", columnNames = {"episode_id", "t"}))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TimeStepDO {

    @ManyToOne
    EpisodeDO episode;

    @OneToMany(cascade = {CascadeType.REMOVE}, mappedBy = "timestep")
    private List<ValueDO> values;

    int t;

    @EqualsAndHashCode.Include
    Integer action;

    float reward;
    float entropy;
    float[] policyTarget;
    int observationPartSize;
    byte[] observationPartA;
    byte[] observationPartB;
    float[] playoutPolicy;
    boolean[] legalActions;
    float rootValueTarget;
    float vMix;
    float rootEntropyValueTarget;
    float rootEntropyValueFromInitialInference;
    float rootValueFromInitialInference;
    float legalActionMaxEntropy;
    boolean exploring;


    boolean archived;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Transient
    double k;



    private double valueMean;
    private double valueHatSquaredMean;
    private long valueCount;


    // policyTarget, observationPartSize, observationPartA, observationPartB are assumed to be immutable
    public TimeStepDO copyPolicyTargetAndObservation() {
        return TimeStepDO.builder()
                .policyTarget(policyTarget)
                .observationPartSize(observationPartSize)
                .observationPartA(observationPartA)
                .observationPartB(observationPartB)
                .episode(episode)
                .t(t)
                .id(id)
                .build();
    }

    public boolean deepEquals(TimeStepDO timeStepDO) {
        if (timeStepDO == null) return false;
        if (timeStepDO == this) return true;
        return
                t == timeStepDO.t &&
                        action == timeStepDO.action &&
                        reward == reward &&
                        entropy == timeStepDO.entropy &&
                        legalActionMaxEntropy == timeStepDO.legalActionMaxEntropy &&
                        rootEntropyValueFromInitialInference == timeStepDO.rootEntropyValueFromInitialInference &&
                        rootEntropyValueTarget == timeStepDO.rootEntropyValueTarget &&
                        rootValueFromInitialInference == timeStepDO.rootValueFromInitialInference &&
                        rootValueTarget == timeStepDO.rootValueTarget &&
                        Arrays.equals(policyTarget, timeStepDO.policyTarget) &&
                        Arrays.equals(legalActions, timeStepDO.legalActions) &&
                        Arrays.equals(playoutPolicy, timeStepDO.playoutPolicy) &&
                        this.getObservation().equals(timeStepDO.getObservation())
                ;
    }

    public TimeStepDO copy() {
        return TimeStepDO.builder()
                .t(t)
                .action(action)
                .reward(reward)
                .entropy(entropy)
                .policyTarget(policyTarget)
                .observation(getObservation())
                .episode(episode)
                .legalActions(legalActions)
                .legalActionMaxEntropy(legalActionMaxEntropy)
                .playoutPolicy(playoutPolicy)
                .rootEntropyValueFromInitialInference(rootEntropyValueFromInitialInference)
                .rootEntropyValueTarget(rootEntropyValueTarget)
                .rootValueFromInitialInference(rootValueFromInitialInference)
                .rootValueTarget(rootValueTarget)
                .build();
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
        } else if (observation == null) {
            observationPartSize = 0;
            observationPartA = null;
            observationPartB = null;
        } else {
            throw new RuntimeException("unknown observation type");
        }
    }


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
}
