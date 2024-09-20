package ai.enpasos.muzero.platform.agent.e_experience.memory2;


import lombok.*;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor

public class ShortEpisode {
    @EqualsAndHashCode.Include
    private Long id;

    private List<ShortTimestep> shortTimesteps;


    public int getUnrollSteps() {
        int t = 0;
        for (;t <= getMaxT(); t++) {
            ShortTimestep shortTimestep = shortTimesteps.get(t);
            if(shortTimestep.isUOkClosed()) {
                if (t == 0) {
                    return getMaxT();
                }
                t = t - 1;
                break;
            }
        }
        return Math.max(1, getMaxT() - t);
    }

    public int getMaxT() {
        return shortTimesteps.size() - 1;
    }

    public boolean isClosed() {
        // if all timesteps are uokclosed, the episode is closed
        return shortTimesteps.stream().allMatch(ShortTimestep::isUOkClosed);
    }

    public boolean hasLowHangingFruits(int unrollSteps) {
        return shortTimesteps.stream().anyMatch(shortTimestep -> shortTimestep.isLowHangingFruit(unrollSteps, this.getMaxT()));
    }
}
