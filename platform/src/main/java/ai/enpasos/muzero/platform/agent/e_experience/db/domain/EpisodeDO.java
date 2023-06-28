package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import ai.enpasos.muzero.platform.common.MuZeroException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;


@Entity
@Table(name = "episode")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EpisodeDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;


  @Fetch(FetchMode.SELECT)
  @BatchSize(size = 10000)
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "episode")
  @EqualsAndHashCode.Include
    private List<TimeStepDO> timeSteps;

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
    long tHybrid = -1;
    int trainingEpoch;
    int tdSteps;



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
            .tHybrid(tHybrid)
            .trainingEpoch(trainingEpoch)
            .tdSteps(tdSteps)
            .timeSteps(timeSteps.stream().map(TimeStepDO::copy).collect(Collectors.toList()))
            .build();
  }

  public EpisodeDO copy(int toPosition) {
    EpisodeDO episodeDO = copy();
    episodeDO.setTimeSteps(episodeDO.getTimeSteps().subList(0, toPosition));
    return episodeDO;
  }


  public Optional<TimeStepDO> getFirstTimeStep() {
    if (timeSteps.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(timeSteps.get(0));
  }

  public @Nullable float getRewardFromLastTimeStep() {
    return    getLastTimeStep().getReward();
  }

  public boolean[] getLegalActionsFromLastTimeStep() {
    return getLastTimeStep().getLegalActions();
  }

  public TimeStepDO getLastTimeStep() {
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
    int t = getLastTime();
    if (this.timeSteps.get(t).getAction() == null) {
      t--;
    }
    if (t >= 0 && this.timeSteps.get(t).getAction() == null) {
      throw new MuZeroException("no action found at time " + t);
    }
    return t;
  }
  public TimeStepDO getLastTimeStepWithAction() {
    int t = getLastTime();
    TimeStepDO timeStepDO = this.timeSteps.get(t);
    if (this.timeSteps.get(t).getAction() == null) {
      t--;
      timeStepDO = this.timeSteps.get(t);
    }
    if (this.timeSteps.get(t).getAction() == null) {
      throw new MuZeroException("no action found at time " + t);
    }
    return timeStepDO;
  }


  public Observation getObservationFromLastTimeStep() {
    int t = getLastTime();
    return timeSteps.get(t).getObservation();
  }



  public TimeStepDO addNewTimeStepDO() {
    if (timeSteps == null) {
      timeSteps = new ArrayList<>();
    }
    int t = this.getLastTime()  + 1;
    TimeStepDO timeStepDO = TimeStepDO.builder().episode(this).t(t).build();
    timeSteps.add(timeStepDO);
    return timeStepDO;
  }



  public EpisodeDO copyWithoutTimeSteps() {
    EpisodeDO copy = new EpisodeDO();
    copy.networkName = this.networkName;
    copy.count = this.count;
    copy.nextSurpriseCheck = this.nextSurpriseCheck;
    copy.tdSteps = this.tdSteps;
    copy.hybrid = this.hybrid;
    copy.tHybrid = this.tHybrid;
    copy.trainingEpoch = this.trainingEpoch;
    copy.pRandomActionRawCount = this.pRandomActionRawCount;
    copy.pRandomActionRawSum = this.pRandomActionRawSum;
    copy.lastValueError = this.lastValueError;
    copy.surprised = this.surprised;
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
    return tHybrid > 0;
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
    if(this.getTimeSteps().size() == 0) {
      return 0;
    } else {
        return this.getTimeSteps().get(0).getEntropy();
    }
  }


}
