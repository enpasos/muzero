package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationOnePlayer;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationTwoPlayers;
import ai.enpasos.muzero.platform.agent.e_experience.box.Boxes;
import jakarta.persistence.*;
import lombok.*;

import java.util.Arrays;
import java.util.BitSet;



@Entity
@Table(name = "timestep",
        uniqueConstraints =
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



    int t;

    @EqualsAndHashCode.Include   // TODO check this does not fit to the unique constraint
    Integer action;

    float reward;

    float rewardLoss;
    float legalActionLossMax;

    int s; // in-mind training steps before this time step, s-1 is the last in-mind step successfully trained


    boolean sClosed;

    @Transient
    boolean sChanged;


    @Transient
    boolean toBeAnalysed;


//    @Builder.Default
//    int boxA = 0;
//
//    @Builder.Default
//    int boxB = 0;


    @Column(name = "boxes", columnDefinition = "integer[]")
    int[] boxes;


    public boolean changeBoxesBasesOnUOk() {
        //this.uOk = uok;
        if (boxes == null) {
            boxes = new int[Math.max(this.uOk, 1)];
         //   boxes[0] = 0;
        }
        return Boxes.toUOk(boxes, this.uOk, uOkClosed, uOkTested);
    }

    @Builder.Default
    int uOk = -2; // unroll steps ok, -2 means not determined, -1 means evens for 0 unrollsteps not ok

    @Builder.Default
    int nextUOk = 100000;   // a large number will not hinder


    int nextuoktarget;

    boolean trainable;

    @Transient
    boolean unrollStepsChanged;


    @Transient
    boolean uOkChanged;

    @Transient
    boolean uOkTested;



    boolean uOkClosed;

    float entropy;
    float[] policyTarget;
    int observationPartSize;
    byte[] observationPartA;
    byte[] observationPartB;
    float[] playoutPolicy;
  //  float[] simState;
    float rootValueTarget;
    float vMix;
    float rootEntropyValueTarget;
    float rootEntropyValueFromInitialInference;
    float rootValueFromInitialInference;

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
                        rewardLoss == timeStepDO.rewardLoss &&
                        legalActionLossMax == timeStepDO.legalActionLossMax &&
                        rootEntropyValueFromInitialInference == timeStepDO.rootEntropyValueFromInitialInference &&
                        rootEntropyValueTarget == timeStepDO.rootEntropyValueTarget &&
                        rootValueFromInitialInference == timeStepDO.rootValueFromInitialInference &&
                        rootValueTarget == timeStepDO.rootValueTarget &&
                        Arrays.equals(policyTarget, timeStepDO.policyTarget) &&
                         legalact.equals(timeStepDO.legalact) &&
                        Arrays.equals(playoutPolicy, timeStepDO.playoutPolicy) &&
                        this.getObservation().equals(timeStepDO.getObservation()) &&
                        Arrays.equals(boxes, timeStepDO.boxes)
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
                .legalActionLossMax(legalActionLossMax)
                .rewardLoss(rewardLoss)
                .playoutPolicy(playoutPolicy)
                .rootEntropyValueFromInitialInference(rootEntropyValueFromInitialInference)
                .rootEntropyValueTarget(rootEntropyValueTarget)
                .rootValueFromInitialInference(rootValueFromInitialInference)
                .rootValueTarget(rootValueTarget)
                .boxes(boxes)
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
