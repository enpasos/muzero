package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Entity
@Table(name = "episode")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EpisodeDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;


  // @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.ALL}, mappedBy = "episode")
  @Fetch(FetchMode.SELECT)
  @BatchSize(size = 10000)
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "episode")
    // @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    // @EqualsAndHashCode.Exclude
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

  public Optional<TimeStepDO> getLastTimeStep() {
    if (timeSteps.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(timeSteps.get(timeSteps.size() - 1));
  }

  public Optional<TimeStepDO> getFirstTimeStep() {
    if (timeSteps.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(timeSteps.get(0));
  }

  public @Nullable Float getLatestReward() {
    return  !getLastTimeStep().isPresent() ? 0f : getLastTimeStep().get().getReward();
  }

  public boolean[] getLatestLegalActions() {
    return  getLastTimeStep().orElseThrow().getLegalActions();
  }

  public int getLastActionTime() {
    return
            timeSteps.stream().filter(timeStepDO -> timeStepDO.getAction() != null)
                    .mapToInt(TimeStepDO::getT)
                    .max()
                    .orElse(-1);
  }
  public OptionalInt getLastLegalActionsTime() {
    return
            timeSteps.stream().filter(timeStepDO -> timeStepDO.getLegalActions() != null)
                    .mapToInt(TimeStepDO::getT)
                    .max();

  }
  public OptionalInt getLastObservationTime() {
    return
            timeSteps.stream().filter(timeStepDO -> timeStepDO.getObservation() != null)
                    .mapToInt(TimeStepDO::getT)
                    .max();

  }

  public Observation getLatestObservation() {
    int t = getLastObservationTime().orElseThrow();
    return timeSteps.get(t).getObservation();
  }

  public void addNewTimeStepDO() {
    if (timeSteps == null) {
      timeSteps = new ArrayList<>();
    }
    timeSteps.add(TimeStepDO.builder().episode(this).build());
  }


  public OptionalInt getLastPolicyTargetsTime() {
    return timeSteps.stream().filter(timeStepDO -> timeStepDO.getPolicyTarget() != null)
                    .mapToInt(TimeStepDO::getT)
                    .max();
  }

  public OptionalInt getLastRootEntropyValuesFromInitialInferenceTime() {
    return timeSteps.stream().filter(timeStepDO -> timeStepDO.getRootEntropyValueFromInitialInference() != null)
            .mapToInt(TimeStepDO::getT)
            .max();
  }

  public OptionalInt getLastRootEntropyValueTargetTime() {
    return timeSteps.stream().filter(timeStepDO -> timeStepDO.getRootEntropyValueTarget() != null)
            .mapToInt(TimeStepDO::getT)
            .max();
  }

  public OptionalInt getLastEntropyTime() {
    return timeSteps.stream().filter(timeStepDO -> timeStepDO.getEntropy() != null)
            .mapToInt(TimeStepDO::getT)
            .max();
  }

  public OptionalInt getLastRootValueFromInitialInferenceTime() {
    return timeSteps.stream().filter(timeStepDO -> timeStepDO.getRootValueFromInitialInference() != null)
            .mapToInt(TimeStepDO::getT)
            .max();
  }

  public OptionalInt getLastRootValueTargetTime() {
    return timeSteps.stream().filter(timeStepDO -> timeStepDO.getRootValueTarget() != null)
            .mapToInt(TimeStepDO::getT)
            .max();
  }

  public OptionalInt getLastVMixTime() {
    return timeSteps.stream().filter(timeStepDO -> timeStepDO.getVMix() != null)
            .mapToInt(TimeStepDO::getT)
            .max();
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
    double entropySum = this.getTimeSteps().stream().filter(timeStepDO -> timeStepDO.getEntropy() != null).mapToDouble(timeStepDO -> timeStepDO.getEntropy()).sum();
    double entropyCount = this.getTimeSteps().stream().filter(timeStepDO -> timeStepDO.getEntropy() != null).count();
    return entropySum / Math.max(1, entropyCount);
  }

  public double getAverageActionMaxEntropy() {
    double sum = this.getTimeSteps().stream().filter(timeStepDO -> timeStepDO.getLegalActionMaxEntropy() != null).mapToDouble(timeStepDO -> timeStepDO.getLegalActionMaxEntropy()).sum();
    double count = this.getTimeSteps().stream().filter(timeStepDO -> timeStepDO.getLegalActionMaxEntropy() != null).count();
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
