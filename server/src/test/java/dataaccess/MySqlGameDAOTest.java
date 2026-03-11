package dataaccess;

import chess.ChessGame;
import model.GameData;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class MySqlGameDAOTest {
    private MySqlGameDAO gameDAO;

    @BeforeAll
    static void init() throws DataAccessException {
        DatabaseManager.configureDatabase();
    }

    @BeforeEach
    void setUp() throws DataAccessException {
        gameDAO = new MySqlGameDAO();
        gameDAO.clear();
    }

    @Test
    @DisplayName("Create and get game with board state")
    void createAndGetGame() throws DataAccessException {
        ChessGame chessGame = new ChessGame();
        GameData game = new GameData(0, null, null, "Test Game", chessGame);
        int gameID = gameDAO.createGame(game);
        assertTrue(gameID > 0);

        GameData result = gameDAO.getGame(gameID);
        assertNotNull(result);
        assertEquals("Test Game", result.gameName());
        assertNotNull(result.game());
        assertEquals(chessGame.getBoard(), result.game().getBoard());
    }

    @Test
    @DisplayName("Get nonexistent game returns null")
    void getNonexistentGame() throws DataAccessException {
        assertNull(gameDAO.getGame(99999));
    }

    @Test
    @DisplayName("List games returns all games")
    void listGames() throws DataAccessException {
        gameDAO.createGame(new GameData(0, null, null, "Game 1", new ChessGame()));
        gameDAO.createGame(new GameData(0, null, null, "Game 2", new ChessGame()));
        gameDAO.createGame(new GameData(0, null, null, "Game 3", new ChessGame()));
        Collection<GameData> games = gameDAO.listGames();
        assertEquals(3, games.size());
    }

    @Test
    @DisplayName("List games empty")
    void listGamesEmpty() throws DataAccessException {
        Collection<GameData> games = gameDAO.listGames();
        assertEquals(0, games.size());
    }

    @Test
    @DisplayName("Update game success")
    void updateGame() throws DataAccessException {
        int gameID = gameDAO.createGame(new GameData(0, null, null, "Test Game", new ChessGame()));
        GameData updated = new GameData(gameID, "alice", "bob", "Test Game", new ChessGame());
        gameDAO.updateGame(updated);

        GameData result = gameDAO.getGame(gameID);
        assertEquals("alice", result.whiteUsername());
        assertEquals("bob", result.blackUsername());
    }

    @Test
    @DisplayName("Update nonexistent game throws")
    void updateNonexistentGame() {
        GameData fake = new GameData(99999, null, null, "Fake", new ChessGame());
        assertThrows(DataAccessException.class, () -> gameDAO.updateGame(fake));
    }

    @Test
    @DisplayName("Clear removes all games")
    void clearGames() throws DataAccessException {
        gameDAO.createGame(new GameData(0, null, null, "Game 1", new ChessGame()));
        gameDAO.createGame(new GameData(0, null, null, "Game 2", new ChessGame()));
        gameDAO.clear();
        Collection<GameData> games = gameDAO.listGames();
        assertEquals(0, games.size());
    }
}
