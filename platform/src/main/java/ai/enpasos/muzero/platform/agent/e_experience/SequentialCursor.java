package ai.enpasos.muzero.platform.agent.e_experience;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
@Slf4j
public class SequentialCursor {
    int batchSize;
    int count = 0;
    int epoch = 0;
    @Builder.Default
    int epochs = Integer.MAX_VALUE;
    private int gameIndex = 0;
    int positionInGame = 0;
    @ToString.Exclude
    List<Path> paths;
    @ToString.Exclude
    Path path;
    @ToString.Exclude
    GameBufferDTO gameBufferDTO;;

    private void increase() {
        count++;
    }

    public void setEpoch(int epoch) {
        if (this.epoch != epoch) {
            setGameIndex(0);
        }
        this.epoch = epoch;
    }

    public void setGameIndex(int gameIndex) {
        if (this.gameIndex != gameIndex) {
            setPositionInGame(0);
        }
        this.gameIndex = gameIndex;
    }

    public void setPositionInGame(int positionInGame) {
        if (this.positionInGame != positionInGame) {
            increase();
        }
        this.positionInGame = positionInGame;
    }

    public boolean hasNext() {
       // return epoch < epochs - 1 && count < 100*batchSize;
        return epoch < epochs - 1  ;
    }


}
