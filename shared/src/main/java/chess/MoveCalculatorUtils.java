package chess;

import java.util.Collection;

public class MoveCalculatorUtils {
    public static void slidingMoves(ChessBoard board, ChessPosition myPosition, int[][] directions, Collection<ChessMove> moves) {
        ChessGame.TeamColor myColor = board.getPiece(myPosition).getTeamColor();
        for (int[] dir : directions) {
            int row = myPosition.getRow();
            int col = myPosition.getColumn();

            while (true) {
                row += dir[0];
                col += dir[1];

                // Stop if off board
                if (row < 1 || row > 8 || col < 1 || col > 8) {
                    break;
                }
                ChessPosition possiblePosition = new ChessPosition(row, col);
                ChessMove possibleMove = new ChessMove(myPosition, possiblePosition, null);
                // Spot is empty
                if (board.getPiece(possiblePosition) == null) {
                    moves.add(possibleMove);
                }
                // Spot has teams color, can't move there or past, break loop
                else if (board.getPiece(possiblePosition).getTeamColor() == myColor) {
                    break;
                }
                // Spot has other teams color, can move there but not past, add move and break loop
                else {
                    moves.add(possibleMove);
                    break;
                }
            }
        }
    }
    public static void staticMoves(ChessBoard board, ChessPosition myPosition, int[][] directions, Collection<ChessMove> moves) {
        ChessGame.TeamColor myColor = board.getPiece(myPosition).getTeamColor();
        for (int[] dir : directions) {
            int row = myPosition.getRow();
            int col = myPosition.getColumn();

            row += dir[0];
            col += dir[1];

            // Stop if off board
            if (row < 1 || row > 8 || col < 1 || col > 8) {
                continue;
            }
            ChessPosition possiblePosition = new ChessPosition(row, col);
            ChessMove possibleMove = new ChessMove(myPosition, possiblePosition, null);
            // Spot is empty or the other teams color
            if (board.getPiece(possiblePosition) == null || board.getPiece(possiblePosition).getTeamColor() != myColor) {
                moves.add(possibleMove);
            }
        }
    }
}
