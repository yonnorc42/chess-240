package server.websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.AuthData;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import java.io.IOException;

@WebSocket
public class WebSocketHandler {
    private final ConnectionManager connections = new ConnectionManager();
    private final AuthDAO authDAO;
    private final GameDAO gameDAO;
    private final Gson gson = new Gson();

    public WebSocketHandler(AuthDAO authDAO, GameDAO gameDAO) {
        this.authDAO = authDAO;
        this.gameDAO = gameDAO;
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
        switch (command.getCommandType()) {
            case CONNECT -> handleConnect(session, command);
            case MAKE_MOVE -> handleMakeMove(session, gson.fromJson(message, MakeMoveCommand.class));
            default -> sendError(session, "Unknown command type");
        }
    }

    private void handleConnect(Session session, UserGameCommand command) throws IOException {
        try {
            AuthData auth = authDAO.getAuth(command.getAuthToken());
            if (auth == null) {
                sendError(session, "Error: unauthorized");
                return;
            }
            GameData gameData = gameDAO.getGame(command.getGameID());
            if (gameData == null) {
                sendError(session, "Error: game not found");
                return;
            }

            String username = auth.username();
            connections.add(username, session, command.getGameID());

            // Send LOAD_GAME to connecting user
            connections.sendTo(username, new LoadGameMessage(gameData.game()));

            // Determine role and notify others
            String role;
            if (username.equals(gameData.whiteUsername())) {
                role = "white";
            } else if (username.equals(gameData.blackUsername())) {
                role = "black";
            } else {
                role = "an observer";
            }
            String notification = username + " joined the game as " + role;
            connections.broadcast(command.getGameID(), username, new NotificationMessage(notification));

        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleMakeMove(Session session, MakeMoveCommand command) throws IOException {
        try {
            AuthData auth = authDAO.getAuth(command.getAuthToken());
            if (auth == null) {
                sendError(session, "Error: unauthorized");
                return;
            }
            GameData gameData = gameDAO.getGame(command.getGameID());
            if (gameData == null) {
                sendError(session, "Error: game not found");
                return;
            }

            String username = auth.username();
            ChessGame game = gameData.game();

            // Check if game is over
            if (game.isOver()) {
                sendError(session, "Error: game is over");
                return;
            }

            // Check if user is a player (not observer)
            ChessGame.TeamColor playerColor = null;
            if (username.equals(gameData.whiteUsername())) {
                playerColor = ChessGame.TeamColor.WHITE;
            } else if (username.equals(gameData.blackUsername())) {
                playerColor = ChessGame.TeamColor.BLACK;
            }
            if (playerColor == null) {
                sendError(session, "Error: observers cannot make moves");
                return;
            }

            // Check if it's the player's turn
            if (game.getTeamTurn() != playerColor) {
                sendError(session, "Error: it is not your turn");
                return;
            }

            // Attempt the move
            ChessMove move = command.getMove();
            game.makeMove(move);

            // Save updated game
            gameDAO.updateGame(new GameData(gameData.gameID(), gameData.whiteUsername(),
                    gameData.blackUsername(), gameData.gameName(), game));

            // Send LOAD_GAME to all clients in game
            LoadGameMessage loadGame = new LoadGameMessage(game);
            connections.broadcast(command.getGameID(), null, loadGame);

            // Notify others of the move
            String moveDesc = formatMove(move);
            connections.broadcast(command.getGameID(), username,
                    new NotificationMessage(username + " moved " + moveDesc));

            // Check for checkmate or stalemate
            ChessGame.TeamColor opponent = (playerColor == ChessGame.TeamColor.WHITE)
                    ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
            if (game.isInCheckmate(opponent)) {
                game.setOver(true);
                gameDAO.updateGame(new GameData(gameData.gameID(), gameData.whiteUsername(),
                        gameData.blackUsername(), gameData.gameName(), game));
                connections.broadcast(command.getGameID(), null,
                        new NotificationMessage(opponent + " is in checkmate"));
            } else if (game.isInStalemate(opponent)) {
                game.setOver(true);
                gameDAO.updateGame(new GameData(gameData.gameID(), gameData.whiteUsername(),
                        gameData.blackUsername(), gameData.gameName(), game));
                connections.broadcast(command.getGameID(), null,
                        new NotificationMessage("Game ended in stalemate"));
            } else if (game.isInCheck(opponent)) {
                connections.broadcast(command.getGameID(), null,
                        new NotificationMessage(opponent + " is in check"));
            }

        } catch (InvalidMoveException e) {
            sendError(session, "Error: invalid move");
        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private String formatMove(ChessMove move) {
        char startCol = (char) ('a' + move.getStartPosition().getColumn() - 1);
        int startRow = move.getStartPosition().getRow();
        char endCol = (char) ('a' + move.getEndPosition().getColumn() - 1);
        int endRow = move.getEndPosition().getRow();
        return "" + startCol + startRow + " to " + endCol + endRow;
    }

    private void sendError(Session session, String message) throws IOException {
        String json = gson.toJson(new ErrorMessage(message));
        session.getRemote().sendString(json);
    }
}
