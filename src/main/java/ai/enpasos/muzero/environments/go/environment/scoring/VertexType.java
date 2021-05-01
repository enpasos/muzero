package ai.enpasos.muzero.environments.go.environment.scoring;

import lombok.Data;


public enum VertexType {

    BlackStone(false),
    WhiteStone(false),
    CapturedBlackStone(true),
    CapturedWhiteStone(true),
    BlackTerritory(true),
    WhiteTerritory(true),
    Dame(true);


    private boolean isTerritory = true;

    public boolean isTerritory() {
        return isTerritory;
    }

    VertexType(boolean isTerritory) {
        this.isTerritory = isTerritory;
    }

}
