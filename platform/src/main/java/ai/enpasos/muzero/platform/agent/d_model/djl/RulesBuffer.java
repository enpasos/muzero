package ai.enpasos.muzero.platform.agent.d_model.djl;

import lombok.Data;

import java.util.Iterator;
import java.util.List;

@Data
public class RulesBuffer {
    List<Long> ids;
    int windowSize;


    public class IdWindowIterator implements Iterator<List<Long>> {
        private int index = 0;

        @Override
        public boolean hasNext() {
            return    index < ids.size();
        }

        @Override
        public List<Long> next() {
            int nextIndex = Math.min(index + windowSize, ids.size());
            List<Long> window = ids.subList(index, nextIndex);
            index = nextIndex;
            return window;
        }
    }

}
