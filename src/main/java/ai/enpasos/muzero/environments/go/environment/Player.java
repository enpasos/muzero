package ai.enpasos.muzero.environments.go.environment;

public enum Player { WhitePlayer, BlackPlayer;

    Player other() {
        switch(this) {
            case BlackPlayer: return WhitePlayer;
            case WhitePlayer:
            default:
                return BlackPlayer;
        }
    }

}


