package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "valuestats", uniqueConstraints =
@UniqueConstraint(name = "UniqueEpochAndTrainingEpoch", columnNames = {"epoch", "trainingEpoch"}))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValueStatsDO {

     @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private int epoch;

    private int trainingEpoch;

    private double vHatSquared;
    private long count;

}
