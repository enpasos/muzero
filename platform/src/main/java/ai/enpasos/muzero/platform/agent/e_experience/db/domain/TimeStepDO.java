package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationOnePlayer;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationTwoPlayers;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;


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

    @ManyToOne
    LegalActionsDO legalact;


    @ManyToOne
    @JoinColumn(name = "statenode_id", nullable = true)
    StateNodeDO statenode;

    @OneToMany(cascade = {CascadeType.REMOVE}, mappedBy = "timestep")
    private List<ValueDO> values;

    int t;

    @EqualsAndHashCode.Include   // TODO check this does not fit to the unique constraint
    Integer action;

    float reward;
    float rewardLoss;
    float entropy;
    float[] policyTarget;
    int observationPartSize;
    byte[] observationPartA;
    byte[] observationPartB;
    float[] playoutPolicy;
    float[] simState;
  //  boolean[] legalActions;
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
    private double valueVariance;
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
                         legalact.equals(timeStepDO.legalact) &&
                        Arrays.equals(playoutPolicy, timeStepDO.playoutPolicy) &&
                        this.getObservation().equals(timeStepDO.getObservation())
                ;
    }

    public TimeStepDO copy() {
        return TimeStepDO.builder()
                .id(id)
                .t(t)
                .action(action)
                .reward(reward)
                .entropy(entropy)
                .policyTarget(policyTarget)
                .observation(getObservation())
                .episode(episode)
                .legalact(legalact)
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

    public TimeStepDO getPreviousTimeStep() {
        if (t == 0) return null;
        return episode.getTimeStep(t - 1);
    }

    public TimeStepDO getNextTimeStep() {
        if (t >= episode.getLastTime()) return null;
        return episode.getTimeStep(t +1);
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


    public void addLegalActions(boolean[] legalActions) {
        LegalActionsDO legalActionsDO =  getLegalact();
        if (legalActionsDO == null) {
            legalActionsDO = new LegalActionsDO();
            this.setLegalact(legalActionsDO);
            legalActionsDO.getTimeSteps().add(this);
        }
        legalActionsDO.setLegalActions(legalActions);
    }
}
