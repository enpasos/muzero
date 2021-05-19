package ai.enpasos.muzero.environments.go.environment;

public enum Player { WhitePlayer, BlackPlayer;

    public Player other() {
        switch(this) {
            case BlackPlayer: return WhitePlayer;
            case WhitePlayer:
            default:
                return BlackPlayer;
        }
    }

}


