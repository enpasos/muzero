package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameDTO;
import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
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


    String networkName = "NONE";
    float pRandomActionRawSum;
    int pRandomActionRawCount;
    float lastValueError;
    long count;
    long nextSurpriseCheck;
    boolean surprised;
    boolean hybrid;
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
            .build();
  }

  public EpisodeDO copy(int toPosition) {
    EpisodeDO episodeDO = copy();
    episodeDO.setTimeSteps(timeSteps.subList(0, toPosition));
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
}
