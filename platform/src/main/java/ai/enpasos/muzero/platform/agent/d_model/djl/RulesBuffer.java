package ai.enpasos.muzero.platform.agent.d_model.djl;

import lombok.Data;

import java.util.Iterator;
import java.util.List;

@Data
public class RulesBuffer {
    List<Long> episodeIds;
    int windowSize;


    public class EpisodeIdsWindowIterator implements Iterator<List<Long>> {
        private int index = 0;

        @Override
        public boolean hasNext() {
            return    index < episodeIds.size();
        }

        @Override
        public List<Long> next() {
            int nextIndex = Math.min(index + windowSize, episodeIds.size());
            List<Long> window = episodeIds.subList(index, nextIndex);
            index = nextIndex;
            return window;
        }
    }

}
