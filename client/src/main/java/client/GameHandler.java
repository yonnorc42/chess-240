package client;

import chess.ChessGame;

public interface GameHandler {
    void onLoadGame(ChessGame game);
    void onNotification(String message);
    void onError(String message);
}
