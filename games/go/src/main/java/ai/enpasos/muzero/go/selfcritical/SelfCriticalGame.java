package ai.enpasos.muzero.go.selfcritical;


import java.util.Map;
import java.util.TreeMap;

public class SelfCriticalGame {
    Map<SelfCriticalPosition, Float> normalizedEntropyValues = new TreeMap<>();
    int firstReliableFullMove;
}
