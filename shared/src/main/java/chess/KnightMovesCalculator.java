package chess;

import java.util.ArrayList;
import java.util.Collection;

import static chess.MoveCalculatorUtils.staticMoves;

public class KnightMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> validMoves = new ArrayList<>();

        int[][] directions = {
                { 2,  1},
                { 2, -1},
                {-2,  1},
                {-2, -1},
                { 1,  2},
                {-1, -2},
                { 1, -2},
                {-1,  2}
        };

        staticMoves(board, myPosition, directions, validMoves);
        return validMoves;
    }
}

