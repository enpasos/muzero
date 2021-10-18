package ai.enpasos.muzero.environments.go.environment;


import ai.enpasos.muzero.environments.go.environment.basics.Player;
import ai.enpasos.muzero.environments.go.environment.basics.Point;
import ai.enpasos.muzero.environments.go.environment.basics.move.Move;
import ai.enpasos.muzero.environments.go.environment.basics.move.Pass;
import ai.enpasos.muzero.environments.go.environment.basics.move.Play;
import ai.enpasos.muzero.environments.go.environment.basics.move.Resign;
import ai.enpasos.muzero.environments.go.environment.scoring.GameResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * GameState encodes the state of a game of Go. Game states have board instances,
 * but also track previous moves to assert validity of moves etc. GameState is
 * immutable, i.e. after you apply a move a new GameState instance will be returned.
 *
 * @param board a GoBoard instance
 * @param nextPlayer the Player who is next to play
 * @param previousState Previous GameState, if any
 * @param lastMove last move played in this game, if any
 * adapted from https://github.com/maxpumperla/ScalphaGoZero
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameState {

    @Builder.Default
    List<Pair<Player, Long>> allPreviousStates = new ArrayList<>();
    private GoBoard board;
    private Player nextPlayer;
    @Builder.Default
    private Optional<GameState> previousState = Optional.empty();
    @Builder.Default
    private Optional<Move> lastMove = Optional.empty();




    public boolean isOver() {
        if (lastMove.isEmpty()) return false;
        if (lastMove.get() instanceof Play) return false;
        if (lastMove.get() instanceof Resign) return true;
        if (lastMove.get() instanceof Pass) {
            var secondLastMove = previousState.get().lastMove;
            if (secondLastMove.isEmpty()) return false;
            if (secondLastMove.get() instanceof Pass) return true;
            if (secondLastMove.get() instanceof Play) return false;
            if (secondLastMove.get() instanceof Resign) return false;
        }
        return false;
    }

    public GameState applyMove(Move move) {
        GoBoard nextBoard = board;
        if (move instanceof Play) {
            nextBoard = board.placeStone(nextPlayer, ((Play) move).getPoint());
        }

        List<Pair<Player, Long>> newAllPrevStates =  new ArrayList<>();
        newAllPrevStates.addAll(allPreviousStates);
        newAllPrevStates.add(Pair.of(nextPlayer, nextBoard.zobristHash()));

        return GameState.builder()
                .board(nextBoard)
                .nextPlayer(nextPlayer.other())
                .previousState(Optional.of(this))
                .lastMove(Optional.of(move))
                .allPreviousStates(newAllPrevStates)
                .build();

    }


    boolean isMoveSelfCapture(Player player, Move move) {
        if (move instanceof Pass || move instanceof Resign) return false;
        return board.isSelfCapture(player, ((Play)move).getPoint());
    }

    boolean doesMoveViolateKo(Player player, Move move) {
        if (move instanceof Pass || move instanceof Resign) return false;
        Play play = (Play)move;
        if (board.willCapture(player, play.getPoint())) {
            var nextBoard = board;
            nextBoard = nextBoard.placeStone(player, play.getPoint());
            nextBoard.zobristHash();
            var nextSituation = Pair.of(player, nextBoard.zobristHash());
            return allPreviousStates.contains(nextSituation);
        }
        return false;
    }

    /** @return true if the move fills a single point eye. Though not strictly illegal, it is never a good idea */
    public boolean doesMoveFillEye(Player player, Move move) {
        if (move instanceof Pass || move instanceof Resign) return false;
        Play play = (Play)move;
        return board.doesMoveFillEye(player, play.getPoint());
    }

    public boolean isValidMove(Move move) {
        if (isOver()) return false;
        if (move instanceof Pass || move instanceof Resign) return true;
        Play play = (Play)move;
        return board.getPlayer(play.getPoint()).isEmpty() &&
                !isMoveSelfCapture(nextPlayer, move) &&
                !doesMoveViolateKo(nextPlayer, move) &&
                !doesMoveFillEye(nextPlayer, move);
    }
    public Optional<GameResult> gameResult() {
        return gameResult(0.5f);
    }

    public Optional<GameResult> gameResult(float komi) {
        if (!isOver()) return Optional.empty();
        if (lastMove.isEmpty() || lastMove.get() instanceof Play || lastMove.get() instanceof Pass ) {
            return Optional.of(GameResult.apply(board, komi));
        } else {
            // Resign
            return Optional.of(GameResult.apply(board, komi, Optional.of(nextPlayer)));
        }


    }


    public List<Move> getValidMoves() {
        List<Move> validMoves = new ArrayList<>();
        for (int i = 1; i <= this.getBoard().getSize(); i++) {
            for (int j = 1; j <= this.getBoard().getSize(); j++) {
                Play play = new Play(new Point(i, j));
                if (isValidMove(play)) {
                    validMoves.add(play);
                }
            }
        }

        Move move = new Pass();
        if (isValidMove(move)) {
            validMoves.add(move);
        }
        move = new Resign();
        if (isValidMove(move)) {
            validMoves.add(move);
        }
        return validMoves;
    }

    public static GameState newGame(int boardSize) {
        return GameState.builder()
                .nextPlayer(Player.BlackPlayer)
                .board(new GoBoard(boardSize))
                .build();
    }

}





