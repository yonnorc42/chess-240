package ui;

import chess.*;

import static ui.EscapeSequences.*;

public class BoardRenderer {

    public static String render(ChessBoard board, ChessGame.TeamColor perspective) {
        StringBuilder sb = new StringBuilder();

        boolean whiteBottom = (perspective == ChessGame.TeamColor.WHITE);

        sb.append(renderColumnHeaders(whiteBottom));

        int startRow = whiteBottom ? 8 : 1;
        int endRow = whiteBottom ? 0 : 9;
        int rowStep = whiteBottom ? -1 : 1;

        for (int row = startRow; row != endRow; row += rowStep) {
            sb.append(renderRow(board, row, whiteBottom));
        }

        sb.append(renderColumnHeaders(whiteBottom));
        sb.append(RESET_BG_COLOR).append(RESET_TEXT_COLOR);

        return sb.toString();
    }

    private static String renderColumnHeaders(boolean whiteBottom) {
        StringBuilder sb = new StringBuilder();
        sb.append(SET_BG_COLOR_LIGHT_GREY).append(SET_TEXT_COLOR_BLACK);
        sb.append(EMPTY);
        String[] cols = whiteBottom
                ? new String[]{"a", "b", "c", "d", "e", "f", "g", "h"}
                : new String[]{"h", "g", "f", "e", "d", "c", "b", "a"};
        for (String c : cols) {
            sb.append(" ").append(c).append(" ");
        }
        sb.append(EMPTY);
        sb.append(RESET_BG_COLOR).append("\n");
        return sb.toString();
    }

    private static String renderRow(ChessBoard board, int row, boolean whiteBottom) {
        StringBuilder sb = new StringBuilder();

        sb.append(SET_BG_COLOR_LIGHT_GREY).append(SET_TEXT_COLOR_BLACK);
        sb.append(" ").append(row).append(" ");

        int startCol = whiteBottom ? 1 : 8;
        int endCol = whiteBottom ? 9 : 0;
        int colStep = whiteBottom ? 1 : -1;

        for (int col = startCol; col != endCol; col += colStep) {
            boolean lightSquare = (row + col) % 2 != 0;
            sb.append(lightSquare ? SET_BG_COLOR_WHITE : SET_BG_COLOR_DARK_GREEN);

            ChessPiece piece = board.getPiece(new ChessPosition(row, col));
            sb.append(pieceToString(piece));
        }

        sb.append(SET_BG_COLOR_LIGHT_GREY).append(SET_TEXT_COLOR_BLACK);
        sb.append(" ").append(row).append(" ");
        sb.append(RESET_BG_COLOR).append("\n");
        return sb.toString();
    }

    private static String pieceToString(ChessPiece piece) {
        if (piece == null) {
            return EMPTY;
        }
        String textColor = (piece.getTeamColor() == ChessGame.TeamColor.WHITE)
                ? SET_TEXT_COLOR_RED : SET_TEXT_COLOR_BLUE;
        String symbol = switch (piece.getPieceType()) {
            case KING -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_KING : BLACK_KING;
            case QUEEN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_QUEEN : BLACK_QUEEN;
            case BISHOP -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_BISHOP : BLACK_BISHOP;
            case KNIGHT -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_KNIGHT : BLACK_KNIGHT;
            case ROOK -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_ROOK : BLACK_ROOK;
            case PAWN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_PAWN : BLACK_PAWN;
        };
        return textColor + symbol;
    }
}
