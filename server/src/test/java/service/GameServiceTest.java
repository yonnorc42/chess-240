package service;

import dataaccess.MemoryAuthDAO;
import dataaccess.MemoryGameDAO;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
    private GameService gameService;
    private MemoryGameDAO gameDAO;
    private MemoryAuthDAO authDAO;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        gameDAO = new MemoryGameDAO();
        authDAO = new MemoryAuthDAO();
        gameService = new GameService(gameDAO, authDAO);
        AuthData auth = new AuthData("valid-token", "alice");
        authDAO.createAuth(auth);
        authToken = auth.authToken();
    }

    @Test
    @DisplayName("Create game success")
    void createGameSuccess() throws ServiceException {
        int gameID = gameService.createGame(authToken, "Test Game");
        assertTrue(gameID > 0);
    }

    @Test
    @DisplayName("Create game unauthorized")
    void createGameUnauthorized() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                gameService.createGame("bad-token", "Test Game"));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    @DisplayName("List games success")
    void listGamesSuccess() throws ServiceException {
        gameService.createGame(authToken, "Game 1");
        gameService.createGame(authToken, "Game 2");
        Collection<GameData> games = gameService.listGames(authToken);
        assertEquals(2, games.size());
    }

    @Test
    @DisplayName("List games unauthorized")
    void listGamesUnauthorized() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                gameService.listGames("bad-token"));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    @DisplayName("Join game success")
    void joinGameSuccess() throws ServiceException {
        int gameID = gameService.createGame(authToken, "Test Game");
        assertDoesNotThrow(() -> gameService.joinGame(authToken, "WHITE", gameID));
    }

    @Test
    @DisplayName("Join game color already taken")
    void joinGameColorTaken() throws ServiceException {
        int gameID = gameService.createGame(authToken, "Test Game");
        gameService.joinGame(authToken, "WHITE", gameID);

        AuthData auth2 = new AuthData("token2", "bob");
        authDAO.createAuth(auth2);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                gameService.joinGame(auth2.authToken(), "WHITE", gameID));
        assertEquals(403, ex.getStatusCode());
    }
}
