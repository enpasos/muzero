package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "legalactions")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LegalActionsDO {

    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "legalact")
    private List<TimeStepDO> timeSteps = new ArrayList<>();


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(unique=true)
    @EqualsAndHashCode.Include
    boolean[] legalActions;

}
