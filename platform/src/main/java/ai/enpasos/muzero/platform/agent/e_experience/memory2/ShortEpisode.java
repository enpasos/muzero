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
        // if there is any timestep in shortTimesteps with uok < 1 return 1
//        if (shortTimesteps.stream().anyMatch(shortTimestep -> shortTimestep.getUOk() < 1 && !shortTimestep.isUOkClosed())) {
//            return 1;
//        }

        int t = getMaxT();
        for (;t >= 0; t--) {
            ShortTimestep shortTimestep = shortTimesteps.get(t);
            if(!shortTimestep.isUOkClosed()) {
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
}
