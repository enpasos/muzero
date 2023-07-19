package ai.enpasos.muzero.platform.agent.e_experience.db.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "valuestats", uniqueConstraints =
@UniqueConstraint(name = "UniqueEpochAndEpisode", columnNames = {"epoch", "episode_id"}))
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValueStatsDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private int epoch;

    private double maxValueHatSquaredMean;
    private int tOfMaxValueHatSquaredMean;

   // private double vHatSquared;
   // private long count;

    @ManyToOne
    EpisodeDO episode;

}
