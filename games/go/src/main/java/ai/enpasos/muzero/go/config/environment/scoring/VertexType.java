package ai.enpasos.muzero.go.config.environment.scoring;


public enum VertexType {

    BLACK_STONE(false),
    WHITE_STONE(false),
    CAPTURED_BLACK_STONE(true),
    CAPTURED_WHITE_STONE(true),
    BLACK_TERRITORY(true),
    WHITE_TERRITORY(true),
    DAME(true);


    private boolean isTerritory = true;

    VertexType(boolean isTerritory) {
        this.isTerritory = isTerritory;
    }

    public boolean isTerritory() {
        return isTerritory;
    }

}
