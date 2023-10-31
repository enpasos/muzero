package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "valuedo", uniqueConstraints =
@UniqueConstraint(name = "UniqueTimestepAndEpoch", columnNames = {"timestep_id", "epoch"}))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValueDO {

    @ManyToOne
    TimeStepDO timestep;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private int epoch;

    private double valuedo;






    boolean archived ;

}