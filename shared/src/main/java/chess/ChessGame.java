package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private ChessBoard board;
    private TeamColor teamTurn;
    private ChessMove lastMove;

    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.teamTurn = TeamColor.WHITE;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        Collection<ChessMove> potentialMoves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> validMoves = new java.util.ArrayList<>();

        for (ChessMove move : potentialMoves) {
            // save current board state
            ChessPiece capturedPiece = board.getPiece(move.getEndPosition());
            board.addPiece(move.getEndPosition(), piece);
            board.addPiece(startPosition, null);

            // check if king is safe
            if (!isInCheck(piece.getTeamColor())) {
                validMoves.add(move);
            }

            // undo the move
            board.addPiece(startPosition, piece);
            board.addPiece(move.getEndPosition(), capturedPiece);
        }

        // en passant check
        TeamColor myColor = board.getPiece(startPosition).getTeamColor();
        int enPassantRow = (myColor == TeamColor.WHITE) ? 5 : 4;
        int verticalDirection = (myColor == TeamColor.WHITE) ? 1 : -1;
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN && startPosition.getRow() == enPassantRow) {
            // check for en passant to the right
            if (canEnPassant(board, startPosition, true)) {
                validMoves.add(new ChessMove(startPosition,
                        new ChessPosition(startPosition.getRow()+verticalDirection, startPosition.getColumn()+1), null));
            }
            // check for en passant to the left
            if (canEnPassant(board, startPosition, false)) {
                validMoves.add(new ChessMove(startPosition,
                        new ChessPosition(startPosition.getRow()+verticalDirection, startPosition.getColumn()-1), null));
            }
        }

        // king castling check, canCastle handles the king being safe logic
        if (piece.getPieceType() == ChessPiece.PieceType.KING && !piece.hasMoved()) {
            // kingside castle
            if (canCastle(board, startPosition, true)) {
                validMoves.add(new ChessMove(startPosition, new ChessPosition(startPosition.getRow(), 7), null));
            }
            // queenside castle
            if (canCastle(board, startPosition, false)) {
                validMoves.add(new ChessMove(startPosition, new ChessPosition(startPosition.getRow(), 3), null));
            }
        }
        return validMoves;
    }
    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        // checks if there's a piece in starting position
        ChessPiece movingPiece = board.getPiece(move.getStartPosition());
        if (movingPiece == null || movingPiece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("No piece of your team at start position");
        }

        // Checks if move is in valid moves
        Collection<ChessMove> moves = validMoves(move.getStartPosition());
        if (moves == null || !moves.contains(move)) {
            throw new InvalidMoveException("Move not valid");
        }

        ChessPiece capturedPiece = board.getPiece(move.getEndPosition());
        ChessPiece.PieceType promotionType = move.getPromotionPiece();

        // en passant special case
        boolean isEnPassant = false;
        if (movingPiece.getPieceType() == ChessPiece.PieceType.PAWN
                && capturedPiece == null
                && move.getStartPosition().getColumn() != move.getEndPosition().getColumn()) {
            // diagonal move without a piece on the target square → en passant
            isEnPassant = true;
            int direction = (teamTurn == TeamColor.WHITE) ? -1 : 1;
            ChessPosition capturedPawnPos = new ChessPosition(
                    move.getEndPosition().getRow() + direction,
                    move.getEndPosition().getColumn()
            );
            capturedPiece = board.getPiece(capturedPawnPos);
            board.addPiece(capturedPawnPos, null); // remove captured pawn
        }

        // execute move (promotion logic here)
        if (promotionType == null) {
            board.addPiece(move.getEndPosition(), movingPiece);
        } else {
            board.addPiece(move.getEndPosition(), new ChessPiece(teamTurn, promotionType));
        }
        board.addPiece(move.getStartPosition(), null);

        // check if move leaves king in check
        if (isInCheck(teamTurn)) {
            // undo en passant if needed
            if (isEnPassant) {
                int direction = (teamTurn == TeamColor.WHITE) ? -1 : 1;
                ChessPosition capturedPawnPos = new ChessPosition(
                        move.getEndPosition().getRow() + direction,
                        move.getEndPosition().getColumn()
                );
                board.addPiece(capturedPawnPos, capturedPiece);
            }

            // undo main move
            board.addPiece(move.getStartPosition(), movingPiece);
            board.addPiece(move.getEndPosition(), capturedPiece);
            throw new InvalidMoveException("Move leaves king in check");
        }
        // handle castling rook move here
        if (movingPiece.getPieceType() == ChessPiece.PieceType.KING) {
            int startCol = move.getStartPosition().getColumn();
            int endCol = move.getEndPosition().getColumn();
            int row = move.getStartPosition().getRow();

            if (startCol == 5 && endCol == 7) { // kingside
                ChessPosition rookStart = new ChessPosition(row, 8);
                ChessPosition rookEnd = new ChessPosition(row, 6);
                ChessPiece rook = board.getPiece(rookStart);
                board.addPiece(rookEnd, rook);
                board.addPiece(rookStart, null);
                rook.setMoved();
            } else if (startCol == 5 && endCol == 3) { // queenside
                ChessPosition rookStart = new ChessPosition(row, 1);
                ChessPosition rookEnd = new ChessPosition(row, 4);
                ChessPiece rook = board.getPiece(rookStart);
                board.addPiece(rookEnd, rook);
                board.addPiece(rookStart, null);
                rook.setMoved();
            }
        }

        // set last move for checking en passant
        lastMove = move;
        // mark piece as moved
        board.getPiece(move.getEndPosition()).setMoved();
        // switch turn
        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }
    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        // get opposite of teamColor
        TeamColor enemyColor = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        // find teamColor king
        ChessPosition kingPos = findKingPosition(teamColor);

        // check if king is attacked
        return isPositionAttacked(enemyColor, kingPos);
    }
    /**
     *
     * @param teamColor team of king we're looking for
     * @return position of king, null if we can't find him, that shouldn't happen
     */
    private ChessPosition findKingPosition(TeamColor teamColor) {
        Collection<ChessPosition> positions = getAllBoardPositions();
        for (ChessPosition pos : positions) {
            ChessPiece piece = board.getPiece(pos);
            // check if there's a piece in each spot. If there is, check if it's a king.
            // If it is, check if it's teamColor. If all are true, that's our king
            if (piece != null && piece.getPieceType() == ChessPiece.PieceType.KING && piece.getTeamColor() == teamColor) {
                return pos;
            }
        }
        // THIS SHOULD NEVER HAPPEN
        return null;
    }
    /**
     *
     * @param attackingColor the enemy team
     * @param attackedPosition the position of the friendly king
     * @return true if friendly king is under attack, otherwise false
     */
    private boolean isPositionAttacked(TeamColor attackingColor, ChessPosition attackedPosition) {
        Collection<ChessPosition> positions = getAllBoardPositions();
        for (ChessPosition pos : positions) {
            if (isEnemyAttacking(attackingColor, pos, attackedPosition)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param attackingColor the enemy team
     * @param pos the pos of the piece potentially attacking king
     * @param target the position of the friendly king
     * @return true if friendly king is under attack by piece in pos, otherwise false
     */
    private boolean isEnemyAttacking(TeamColor attackingColor, ChessPosition pos, ChessPosition target) {
        ChessPiece piece = board.getPiece(pos);
        if (piece == null || piece.getTeamColor() != attackingColor) {
            return false;
        }
        for (ChessMove move : piece.pieceMoves(board, pos)) {
            if (move.getEndPosition().equals(target)) {
                return true;
            }
        }
        return false;
    }
    /**
     *
     * @param board chess board
     * @param  kingPos kings starting position
     * @param kingside boolean that says which direction king is trying to castle
     * @return boolean if the king can castle to that side
     */
    private boolean canCastle(ChessBoard board, ChessPosition kingPos, boolean kingside) {
        int row = kingPos.getRow();
        int rookCol = kingside ? 8 : 1;
        int step = kingside ? 1 : -1;
        TeamColor enemyColor = (board.getPiece(kingPos).getTeamColor() == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        // check that the rook is in original place and hasn't moved
        ChessPiece rook = board.getPiece(new ChessPosition(row, rookCol));
        if (rook == null || rook.getPieceType() != ChessPiece.PieceType.ROOK || rook.hasMoved()) {
            return false;
        }

        // check that the squares between the rook and king are empty
        for (int col = kingPos.getColumn() + step; col != rookCol; col += step) {
            if (board.getPiece(new ChessPosition(row, col)) != null) {
                return false;
            }
        }
        // check that none of the positions that the king has to move into or end on would be under check
        for (int col = kingPos.getColumn(); col != kingPos.getColumn() + 3 * step; col += step) {
            if (isPositionAttacked(enemyColor, new ChessPosition(row, col))) {
                return false;
            }
        }
        return true;
    }
    /**
     *
     * @param board our chess board
     * @param pawnPos starting position of our capturing pawn
     * @param rightSide if the en passant is to the right or not
     * @return if en passant is a valid move here
     */
    private boolean canEnPassant(ChessBoard board, ChessPosition pawnPos, boolean rightSide) {
        // Use lastMove to determine en passant eligibility and simulate using actual pieces (restore afterward).
        int row = pawnPos.getRow();
        int col = pawnPos.getColumn();

        // ensure adjacent file exists
        int adjCol = rightSide ? col + 1 : col - 1;
        if (adjCol < 1 || adjCol > 8) {
            return false;
        }

        ChessPosition adjPos = new ChessPosition(row, adjCol);
        ChessPiece adjPiece = board.getPiece(adjPos);
        ChessPiece myPawn = board.getPiece(pawnPos);

        // must have a pawn of the opponent adjacent
        if (myPawn == null || adjPiece == null) {
            return false;
        }
        if (adjPiece.getPieceType() != ChessPiece.PieceType.PAWN) {
            return false;
        }
        if (adjPiece.getTeamColor() == myPawn.getTeamColor()) {
            return false;
        }
        // lastMove must exist and must be the pawn that moved to the adjacent square by a 2-square jump
        if (lastMove == null) {
            return false;
        }
        if (!lastMove.getEndPosition().equals(adjPos)) {
            return false;
        }
        // the piece that moved must have been a pawn and must have moved two squares
        int startRow = lastMove.getStartPosition().getRow();
        int endRow = lastMove.getEndPosition().getRow();
        if (Math.abs(endRow - startRow) != 2) {
            return false;
        }

        // Now simulate en passant capture and ensure it doesn't leave our king in check
        TeamColor myColor = myPawn.getTeamColor();
        int verticalDirection = (myColor == TeamColor.WHITE) ? 1 : -1;
        ChessPosition targetPos = new ChessPosition(row + verticalDirection, adjCol);

        ChessPiece targetOccupant = board.getPiece(targetPos);

        // perform simulation: move pawn to target, remove original and captured pawn
        board.addPiece(targetPos, myPawn);
        board.addPiece(pawnPos, null);
        board.addPiece(adjPos, null);

        boolean leavesKingInCheck = isInCheck(myColor);

        // undo simulation
        board.addPiece(pawnPos, myPawn);
        board.addPiece(adjPos, adjPiece);
        board.addPiece(targetPos, targetOccupant);

        return !leavesKingInCheck;
    }
    /**
     *
     * @return a list of all the positions on the board
     */
    private Collection<ChessPosition> getAllBoardPositions() {
        Collection<ChessPosition> positions = new ArrayList<>();
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                positions.add(new ChessPosition(row, col));
            }
        }
        return positions;
    }
    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
            return false;
        }

        for (ChessPosition pos : getAllBoardPositions()) {
            ChessPiece piece = board.getPiece(pos);
            if (piece != null && piece.getTeamColor() == teamColor) {
                if (hasEscapingMove(pos, piece, teamColor)) {
                    return false;
                }
            }
        }
        return true;
    }
    /**
     *
     * @param startPos the starting position of the piece attempting to move
     * @param piece the piece attempting to move
     * @param teamColor which team is under threat
     * @return true if the king can escape, false otherwise
     */
    private boolean hasEscapingMove(ChessPosition startPos, ChessPiece piece, TeamColor teamColor) {
        Collection<ChessMove> moves = validMoves(startPos);
        if (moves == null) {
            return false;
        }

        for (ChessMove move : moves) {
            if (simulateMoveDoesEscapeCheck(startPos, piece, move, teamColor)) {
                return true;
            }
        }
        return false;
    }
    /**
     *
     * @param startPos the start position of piece attempting to move
     * @param piece the piece moving
     * @param move the move it makes
     * @param teamColor which team to check for check
     * @return True if the king escapes check with move, otherwise false
     */
    private boolean simulateMoveDoesEscapeCheck(ChessPosition startPos, ChessPiece piece, ChessMove move, TeamColor teamColor) {
        ChessPiece captured = board.getPiece(move.getEndPosition());

        board.addPiece(move.getEndPosition(), piece);
        board.addPiece(startPos, null);

        boolean escapesCheck = !isInCheck(teamColor);

        board.addPiece(startPos, piece);
        board.addPiece(move.getEndPosition(), captured);

        return escapesCheck;
    }
    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }
        for (ChessPosition pos : getAllBoardPositions()) {
            ChessPiece piece = board.getPiece(pos);
            if (piece != null && piece.getTeamColor() == teamColor) {
                if (hasEscapingMove(pos, piece, teamColor)) {
                    return false;
                }
            }
        }
        return true;
    }
    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }
    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return Objects.equals(board, chessGame.board) && teamTurn == chessGame.teamTurn;
    }
    @Override
    public int hashCode() {
        return Objects.hash(board, teamTurn);
    }
}