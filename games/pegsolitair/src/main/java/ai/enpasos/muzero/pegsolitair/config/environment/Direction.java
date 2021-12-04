package ai.enpasos.muzero.pegsolitair.config.environment;

public enum Direction {
    N, E, S, W;


    public Direction reverse() {
        switch (this) {
            case N:
                return S;
            case E:
                return W;
            case S:
                return N;
            case W:
            default:
                return E;
        }
    }
}
