package ai.enpasos.muzero.go.config.environment.scoring;


public enum VertexType {

    BlackStone(false),
    WhiteStone(false),
    CapturedBlackStone(true),
    CapturedWhiteStone(true),
    BlackTerritory(true),
    WhiteTerritory(true),
    Dame(true);


    private boolean isTerritory = true;

    VertexType(boolean isTerritory) {
        this.isTerritory = isTerritory;
    }

    public boolean isTerritory() {
        return isTerritory;
    }

}
