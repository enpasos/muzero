package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import ai.enpasos.muzero.platform.common.MuZeroException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Entity
@Table(name = "episode")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EpisodeDO {

    @Builder.Default
    String networkName = "NONE";
    float pRandomActionRawSum;
    int pRandomActionRawCount;
    float lastValueError;
    long count;
    long nextSurpriseCheck;
    boolean surprised;
    boolean hybrid;
    @Builder.Default
    long tStartNormal = -1;


    // start with single rewarding on an episode
    // to have the same sign and t as the reward information from the model
    // use the value function result on the last timestep
    float environmentRewardValue;
    float modelMemorizedRewardValue;
    boolean modelMemorizedRewardValueSet;


    boolean archived;


    int trainingEpoch;
    int tdSteps;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 10000)
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, mappedBy = "episode")
    private List<TimeStepDO> timeSteps;


    private double maxValueVariance;
    private int tOfMaxValueVariance;
    private int valueCount;


    public String getActionString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{");
        for(TimeStepDO ts :  this.getTimeSteps() ) {
            if (ts.action != null) {
                buf.append(ts.action);
                buf.append(",");
            }
        }
        buf.append("}");
        return  buf.toString();
    }


    private void sortTimeSteps() {
        timeSteps.sort(Comparator.comparing(TimeStepDO::getT));
    }

    public EpisodeDO copy() {
        return EpisodeDO.builder()
                .id(id)
                .networkName(networkName)
                .pRandomActionRawSum(pRandomActionRawSum)
                .pRandomActionRawCount(pRandomActionRawCount)
                .lastValueError(lastValueError)
                .count(count)
                .nextSurpriseCheck(nextSurpriseCheck)
                .surprised(surprised)
                .hybrid(hybrid)
                .tStartNormal(tStartNormal)
                .trainingEpoch(trainingEpoch)
                .tdSteps(tdSteps)
                .timeSteps(timeSteps.stream().map(TimeStepDO::copy).collect(Collectors.toList()))
                .build();
    }

    public EpisodeDO copy(int toPosition) {
        EpisodeDO episodeDO = copy();
        sortTimeSteps();
        episodeDO.setTimeSteps(this.getTimeSteps().subList(0, toPosition));
        return episodeDO;
    }


    public Optional<TimeStepDO> getFirstTimeStep() {
        sortTimeSteps();
        if (timeSteps.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(timeSteps.get(0));
    }

    public @Nullable float getRewardFromLastTimeStep() {
        return getLastTimeStep().getReward();
    }

    public boolean[] getLegalActionsFromLatestTimeStepWithoutAction() {
        int t = getLastTimeWithAction();
        t++;
        return getTimeStep(t).getLegalActions();
    }

    public TimeStepDO getLastTimeStep() {
        sortTimeSteps();
        if (timeSteps.isEmpty()) {
            return null;
        }
        return timeSteps.get(timeSteps.size() - 1);
    }

    public int getLastTime() {
        if (timeSteps.isEmpty()) {
            return -1;
        }
        return getLastTimeStep().getT();
    }

    public int getLastTimeWithAction() {
        sortTimeSteps();
        int t = getLastTime();
        for (int i = t; i >= 0; i--) {
            if (this.timeSteps.get(i).getAction() != null) {
                return i;
            }
        }
        return -1;
    }

    public TimeStepDO getLastTimeStepWithAction() {
        sortTimeSteps();
        int t0 = getLastTime();
        for (int t = t0; t >= 0; t--) {
            TimeStepDO timeStepDO = this.timeSteps.get(t);
            if (this.timeSteps.get(t).getAction() != null) {
                return timeStepDO;
            }
        }
        return null;
    }


    public Observation getObservationFromLastActionTimeStep() {
        int t = getLastTimeWithAction();
        return timeSteps.get(t).getObservation();
    }


    public TimeStepDO addNewTimeStepDO() {
        if (timeSteps == null) {
            timeSteps = new ArrayList<>();
        }
        int t = this.getLastTime() + 1;
        TimeStepDO timeStepDO = TimeStepDO.builder().episode(this).t(t).build();
        timeSteps.add(timeStepDO);
        return timeStepDO;
    }


    public EpisodeDO copyWithoutTimeSteps() {
        EpisodeDO copy = new EpisodeDO();
        copy.timeSteps = new ArrayList<>();
        copy.networkName = this.networkName;
        copy.count = this.count;
        copy.nextSurpriseCheck = this.nextSurpriseCheck;
        copy.tdSteps = this.tdSteps;
        copy.hybrid = this.hybrid;
        copy.tStartNormal = this.tStartNormal;
        copy.trainingEpoch = this.trainingEpoch;
        copy.pRandomActionRawCount = this.pRandomActionRawCount;
        copy.pRandomActionRawSum = this.pRandomActionRawSum;
        copy.lastValueError = this.lastValueError;
        copy.surprised = this.surprised;
        copy.id = this.id;
        return copy;
    }

    public boolean deepEquals(EpisodeDO episodeDO) {
        if (episodeDO == null) {
            return false;
        }
        if (episodeDO.timeSteps.size() != this.timeSteps.size()) {
            return false;
        }
        for (int i = 0; i < episodeDO.timeSteps.size(); i++) {
            if (!episodeDO.timeSteps.get(i).deepEquals(this.timeSteps.get(i))) {
                return false;
            }
        }
        return true;
    }


    public boolean hasExploration() {
        return tStartNormal > 0;
    }

    public double getAverageEntropy() {
        double entropySum = this.getTimeSteps().stream().mapToDouble(timeStepDO -> timeStepDO.getEntropy()).sum();
        double entropyCount = this.getTimeSteps().stream().count();
        return entropySum / Math.max(1, entropyCount);
    }

    public double getAverageActionMaxEntropy() {
        double sum = this.getTimeSteps().stream().mapToDouble(timeStepDO -> timeStepDO.getLegalActionMaxEntropy()).sum();
        double count = this.getTimeSteps().stream().count();
        return sum / Math.max(1, count);

    }

    public List<Integer> getActions() {
        return getTimeSteps().stream().filter(timeStepDO -> timeStepDO.getAction() != null)
                .map(timeStepDO -> timeStepDO.getAction()).collect(Collectors.toList());
    }


    public double getEntropyOfInitialState() {
        if (this.getTimeSteps().size() == 0) {
            return 0;
        } else {
            return this.getTimeSteps().get(0).getEntropy();
        }
    }

    // TODO faster search ... use map
    public TimeStepDO getTimeStep(int t) {

        for (int i = 0; i < timeSteps.size(); i++) {
            TimeStepDO timeStepDO = timeSteps.get(i);
            if (timeStepDO.getT() == t) {
                return timeStepDO;
            }
        }
        throw new MuZeroException("no timestep found for t=" + t);
    }

    public void removeTheLastAction() {
        // if (this.timeSteps.size() == 0) return;
        TimeStepDO timeStepDO = this.getLastTimeStepWithAction();
        if (timeStepDO != null) {
            timeStepDO.setAction(null);
        }

    }


    public int getAction() {
        return this.getLastTimeStepWithAction().getAction();
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof EpisodeDO)) return false;
        final EpisodeDO other = (EpisodeDO) o;
    //    final List<TimeStepDO>  timeSteps1 = this.getTimeSteps();
   //     final List<TimeStepDO>  timeSteps2 = other.getTimeSteps();
        // https://hibernate.atlassian.net/browse/HHH-5409
        // Hibernates PersistentBag does not implement equals correctly
return this.getActionString().equals(other.getActionString());

//        if (timeSteps1 == null ?  timeSteps2 != null : !timeSteps1.equals( timeSteps2)) return false;
//        return true;
    }



    public int hashCode() {
        // https://hibernate.atlassian.net/browse/HHH-5409
        // Hibernates PersistentBag does not implement equals correctly
        return getActionString().hashCode();
//        final int PRIME = 59;
//        int result = 1;
//        final Object $timeSteps = this.getTimeSteps();
//        result = result * PRIME + ($timeSteps == null ? 43 : $timeSteps.hashCode());
//        return result;
    }
}
