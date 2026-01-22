package chess;

import java.util.ArrayList;
import java.util.Collection;

import static chess.MoveCalculatorUtils.slidingMoves;

public class BishopMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> validMoves = new ArrayList<>();

        int[][] directions = {
                {-1,  1},
                {-1, -1},
                { 1,  1},
                { 1, -1}
        };

        slidingMoves(board, myPosition, directions, validMoves);

        return validMoves;
    }
}
