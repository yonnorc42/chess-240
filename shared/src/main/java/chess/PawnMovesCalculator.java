package chess;

import java.util.ArrayList;
import java.util.Collection;

public class PawnMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> validMoves = new ArrayList<>();

        ChessGame.TeamColor myColor = board.getPiece(myPosition).getTeamColor();
        int direction = (myColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int startRow = (myColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
        int promotionRow = (myColor == ChessGame.TeamColor.WHITE) ? 8 : 1;

        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        ChessPosition forwardPosition = new ChessPosition(row + direction, col);
        ChessPosition doubleForwardPosition = new ChessPosition(row + 2*direction, col);
        ChessPosition leftPosition = new ChessPosition(row + direction, col - 1);
        ChessPosition rightPosition = new ChessPosition(row + direction, col + 1);

        // check if spot in front is empty
        if (board.getPiece(forwardPosition) == null) {
            // check if in promotion position
            if (row == promotionRow - direction) {
                addPromotionMoves(validMoves, myPosition, forwardPosition);
            }
            // if not, add the forward move without promotion and check if in starting position
            else {
                validMoves.add(new ChessMove(myPosition, forwardPosition, null));
                if (row == startRow && board.getPiece(doubleForwardPosition) == null) {
                    validMoves.add(new ChessMove(myPosition, doubleForwardPosition, null));
                }
            }

        }
        // check if spot in front and to left is an enemy piece
        if (col > 1 && board.getPiece(leftPosition) != null &&
                board.getPiece(leftPosition).getTeamColor() != myColor) {
            if (row == promotionRow - direction) {
                addPromotionMoves(validMoves, myPosition, leftPosition);
            }
            else {
                validMoves.add(new ChessMove(myPosition, leftPosition, null));
            }
        }
        // check if spot in front and to right is an enemy piece
        if (col < 8 && board.getPiece(rightPosition) != null &&
                board.getPiece(rightPosition).getTeamColor() != myColor) {
            if (row == promotionRow - direction) {
                addPromotionMoves(validMoves, myPosition, rightPosition);
            }
            else {
                validMoves.add(new ChessMove(myPosition, rightPosition, null));
            }
        }
        return validMoves;
    }

    private void addPromotionMoves(Collection<ChessMove> moves, ChessPosition startPosition, ChessPosition endPosition) {
        moves.add(new ChessMove(startPosition, endPosition, ChessPiece.PieceType.QUEEN));
        moves.add(new ChessMove(startPosition, endPosition, ChessPiece.PieceType.ROOK));
        moves.add(new ChessMove(startPosition, endPosition, ChessPiece.PieceType.BISHOP));
        moves.add(new ChessMove(startPosition, endPosition, ChessPiece.PieceType.KNIGHT));
    }

}
