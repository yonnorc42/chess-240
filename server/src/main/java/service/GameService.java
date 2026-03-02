package service;

import chess.ChessGame;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.AuthData;
import model.GameData;

import java.util.Collection;

public class GameService {
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    public int createGame(String authToken, String gameName) throws ServiceException {
        authenticate(authToken);
        if (gameName == null) {
            throw new ServiceException(400, "Error: bad request");
        }
        try {
            GameData game = new GameData(0, null, null, gameName, new ChessGame());
            return gameDAO.createGame(game);
        } catch (DataAccessException e) {
            throw new ServiceException(500, "Error: " + e.getMessage());
        }
    }

    public Collection<GameData> listGames(String authToken) throws ServiceException {
        authenticate(authToken);
        try {
            return gameDAO.listGames();
        } catch (DataAccessException e) {
            throw new ServiceException(500, "Error: " + e.getMessage());
        }
    }

    public void joinGame(String authToken, String playerColor, Integer gameID) throws ServiceException {
        AuthData auth = authenticate(authToken);
        if (gameID == null || playerColor == null || playerColor.isEmpty()) {
            throw new ServiceException(400, "Error: bad request");
        }
        ChessGame.TeamColor color;
        try {
            color = ChessGame.TeamColor.valueOf(playerColor);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(400, "Error: bad request");
        }
        try {
            GameData game = gameDAO.getGame(gameID);
            if (game == null) {
                throw new ServiceException(400, "Error: bad request");
            }
            String username = auth.username();
            GameData updated;
            if (color == ChessGame.TeamColor.WHITE) {
                if (game.whiteUsername() != null) {
                    throw new ServiceException(403, "Error: already taken");
                }
                updated = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
            } else {
                if (game.blackUsername() != null) {
                    throw new ServiceException(403, "Error: already taken");
                }
                updated = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
            }
            gameDAO.updateGame(updated);
        } catch (DataAccessException e) {
            throw new ServiceException(500, "Error: " + e.getMessage());
        }
    }

    private AuthData authenticate(String authToken) throws ServiceException {
        try {
            AuthData auth = authDAO.getAuth(authToken);
            if (auth == null) {
                throw new ServiceException(401, "Error: unauthorized");
            }
            return auth;
        } catch (DataAccessException e) {
            throw new ServiceException(500, "Error: " + e.getMessage());
        }
    }
}
