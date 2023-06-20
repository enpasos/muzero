package ai.enpasos.muzero.platform.agent.e_experience.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


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

}
