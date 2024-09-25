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


    private boolean needsFullTesting;

    @Builder.Default
    private int currentUnrollSteps = 1;

    public int getUnrollSteps() {
        int newUnrollSteps = 1;
        int t = 0;
        for (;t <= getMaxT(); t++) {
            ShortTimestep shortTimestep = shortTimesteps.get(t);
            if(shortTimestep.isUOkClosed()) {
                if (t == 0) {
                    // newUnrollSteps = getMaxT();
                    break;
                }
                t = t - 1;
                break;
            }
        }
        newUnrollSteps = Math.max(1, getMaxT() - t);
        if (newUnrollSteps != currentUnrollSteps) {
            currentUnrollSteps = newUnrollSteps;
            needsFullTesting = true;
        }
        return newUnrollSteps;
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
